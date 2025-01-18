package dev.nateweisz.seacats.queue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.nateweisz.seacats.fleets.Fleet;
import dev.nateweisz.seacats.fleets.FleetService;
import dev.nateweisz.seacats.fleets.ShipInfo;
import dev.nateweisz.seacats.logs.FleetLogType;
import dev.nateweisz.seacats.logs.QueueLogType;
import dev.nateweisz.seacats.messages.MessageService;

import io.github.freya022.botcommands.api.components.Buttons;
import io.github.freya022.botcommands.api.core.BContext;
import io.github.freya022.botcommands.api.core.service.annotations.BService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

@BService
public class PendingTimeoutService {
    private static final Logger logger = LoggerFactory.getLogger(PendingTimeoutService.class);
    private final MessageService messageService;

    @Value("${jda.guildId}")
    public long guildId;

    private final V2QueuedService queueService;
    private final BContext context;
    private final FleetService fleetService;
    private final Buttons buttons;

    public PendingTimeoutService(
            V2QueuedService queueService, BContext context, FleetService fleetService,
            MessageService messageService, Buttons buttons
    ) {
        this.queueService = queueService;
        this.context = context;
        this.fleetService = fleetService;
        this.messageService = messageService;
        this.buttons = buttons;
    }

    @Scheduled(initialDelay = 10, fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    public void clearPendingQueue() {
        try {
            JDA jda = context.getService(JDA.class);
            Guild guild = jda.getGuildById(guildId);

            AtomicBoolean anyChanges = new AtomicBoolean(false);

            // pending removal after timeout
            queueService.getPendingMembers().stream()
                    .filter(QueuedUser::isPendingTimeoutOver)
                    .forEach(user -> {
                        Fleet fleet =
                                fleetService.getFleetByCategory(
                                        jda.getVoiceChannelById(user.getPendingChannel()).getParentCategoryIdLong()
                                );
                        ShipInfo shipInfo = fleet.getChannelShipInfo().get(user.getPendingChannel());

                        jda.getGuildById(guildId).removeRoleFromMember(
                                UserSnowflake.fromId(user.getMemberId()), guild.getRoleById(fleet.getRoleId())
                        ).queue();
                        jda.getGuildById(guildId)
                                .removeRoleFromMember(
                                        UserSnowflake.fromId(user.getMemberId()),
                                        guild.getRoleById(shipInfo.getRoleId())
                                ).queue();

                        user.setPendingChannel(null);
                        user.setPendingTimeout(null);
                        user.setSentReminder(false);

                        // add to afk check
                        // dont remove yet
                        user.setAfkTimeout(System.currentTimeMillis());
                        jda.openPrivateChannelById(user.getMemberId()).queue(channel -> {
                            var remainInQueueButton = buttons.success("Remain In Queue")
                                    .persistent()
                                    .timeout(Duration.ofMinutes(5))
                                    .bindTo("remainInQueueButton")
                                    .build();

                            var leaveInQueueButton = buttons.danger("Leave Queue")
                                    .persistent()
                                    .timeout(Duration.ofMinutes(5))
                                    .bindTo("leaveQueueButton")
                                    .build();

                            channel.sendMessage("Meow! It looks like you missed your queue reservation and can no longer join at this moment. But don’t worry, you’ve still got 5 minutes to use the button below and stay in the queue—after that, you'll be whisked away! \uD83D\uDC3E")
                                    .setActionRow(remainInQueueButton, leaveInQueueButton)
                                    .queue();
                        });


                        queueService.updateQueuedUser(user);

                        MDC.put("type", "queue");
                        MDC.put("queue_type", QueueLogType.MISSED_RESERVATION.name());
                        MDC.put("user_id", String.valueOf(user.getMemberId()));
                        logger.info("Member {} was removed from pending queue", user.getMemberId());
                        MDC.clear();

                        anyChanges.set(true);
                    });

            // pending removal after they leave a vc
            queueService.getPendingRemovals().stream()
                    .filter(QueuedUser::isPendingRemovalOver)
                    .forEach(user -> {
                        Fleet fleet =
                                fleetService.getAllFleets().stream()
                                        .filter(
                                                f -> f.getChannelShipInfo().values().stream()
                                                        .anyMatch(info -> info.getMemberIds().contains(user.getMemberId()))
                                        ).findFirst().orElse(null);

                        if (fleet == null) {
                            user.setPendingRemoval(null);
                            queueService.updateQueuedUser(user);
                            logger.info("Fleet was not found for user {} but they are in pending removal", user.getMemberId());
                            return;
                        }

                        ShipInfo shipInfo =
                                fleet.getChannelShipInfo().values().stream()
                                        .filter(info -> info.getMemberIds().contains(user.getMemberId()))
                                        .findFirst().orElse(null);

                        shipInfo.getMemberIds().remove(user.getMemberId());
                        jda.getGuildById(guildId).removeRoleFromMember(
                                UserSnowflake.fromId(user.getMemberId()), guild.getRoleById(fleet.getRoleId())
                        ).queue();
                        jda.getGuildById(guildId)
                                .removeRoleFromMember(
                                        UserSnowflake.fromId(user.getMemberId()),
                                        guild.getRoleById(shipInfo.getRoleId())
                                ).queue();

                        fleetService.saveFleet(fleet);

                        user.setPendingRemoval(null);
                        queueService.updateQueuedUser(user);

                        MDC.put("type", "fleet");
                        MDC.put("fleet_type", FleetLogType.LEAVE.name());
                        MDC.put("ship","F%s Ship %s".formatted(fleet.getNumericalId(), shipInfo.getShipId()));
                        MDC.put("user_id", String.valueOf(user.getMemberId()));
                        logger.info("Member {} was removed from pending removal", user.getMemberId());
                        MDC.clear();

                        logger.info("Member {} was removed from pending removal", user.getMemberId());

                        anyChanges.set(true);
                    });

            // Check if there is anyone with a minute left in their thing and has not been sent a notification
            queueService.getPendingMembers().stream()
                    .filter(user -> user.isPending() && !user.isSentReminder() && user.getPendingTimeout() != null)
                    .filter(user -> System.currentTimeMillis() - user.getPendingTimeout() > TimeUnit.MINUTES.toMillis(2))
                    .forEach(user -> {
                        user.setSentReminder(true);
                        queueService.updateQueuedUser(user);

                        // Open dm to send reminder
                        jda.retrieveUserById(user.getMemberId()).queue(member -> {
                            member.openPrivateChannel().queue(channel -> {
                                channel.sendMessage(":arrows_counterclockwise: You have 1 minute left to be processed onto this ship before you will be marked as AFK.").queue();
                            });
                        });
                    });

            queueService.getPendingAfk().stream()
                    .filter(QueuedUser::isAfkTimeoutOver)
                    .forEach(queuedUser -> {
                        queuedUser.setAfkTimeout(null);
                        queuedUser.setQueueNote(null);
                        queuedUser.setPriorityReason(null);
                        queuedUser.setQueueTime(null);
                        queuedUser.setStaffNote(null);
                        queuedUser.setType(null);

                        queueService.updateQueuedUser(queuedUser);
                        anyChanges.set(true);
                    });

            if (anyChanges.get()) {
                messageService.updatePanelEmbed(jda);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
