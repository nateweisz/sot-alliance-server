package dev.nateweisz.seacats.listeners;

import java.util.Optional;

import dev.nateweisz.seacats.bot.EmbedService;
import dev.nateweisz.seacats.fleets.Fleet;
import dev.nateweisz.seacats.fleets.FleetService;
import dev.nateweisz.seacats.logs.FleetLogType;
import dev.nateweisz.seacats.messages.MessageRepository;
import dev.nateweisz.seacats.messages.MessageService;
import dev.nateweisz.seacats.queue.QueuedUser;
import dev.nateweisz.seacats.queue.V2QueuedService;

import io.github.freya022.botcommands.api.core.annotations.BEventListener;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QueueJoinListener {
    private static final Logger logger = LoggerFactory.getLogger(QueueJoinListener.class);
    private final MessageService messageService;

    @Value("${jda.guildId}")
    public long guildId;

    private final V2QueuedService queueService;
    private final FleetService fleetService;

    public QueueJoinListener(
        V2QueuedService queueService, FleetService fleetService, MessageService messageService
    ) {
        this.queueService = queueService;
        this.fleetService = fleetService;
        this.messageService = messageService;
    }

    @BEventListener
    public void onReady(ReadyEvent event) {
        messageService.updatePanelEmbed(event.getJDA());
    }

    @BEventListener
    public void onChannelUpdate(GuildVoiceUpdateEvent event) {

        VoiceChannel voiceChannel;

        if (event.getChannelJoined() != null && event.getChannelLeft() != null) {
            // Check if they are moving into a fleet channel
            voiceChannel = event.getChannelJoined().asVoiceChannel();
            Fleet fleet = fleetService.getFleetByCategory(voiceChannel.getParentCategoryIdLong());
            if (fleet != null) {
                handleJoin(fleet, voiceChannel, event);
            } else {
                voiceChannel = event.getChannelLeft().asVoiceChannel();
                fleet = fleetService.getFleetByCategory(voiceChannel.getParentCategoryIdLong());
                if (fleet != null) {
                    handleLeft(fleet, voiceChannel, event);
                }
            }

            // Check if they are moving out of a fleet channel
            return;
        }

        voiceChannel =
            event.getChannelJoined() == null
                ? event.getChannelLeft().asVoiceChannel()
                : event.getChannelJoined().asVoiceChannel();
        Fleet fleet = fleetService.getFleetByCategory(voiceChannel.getParentCategoryIdLong());
        if (fleet == null) {
            return;
        }

        if (event.getChannelJoined() != null) {
            handleJoin(fleet, voiceChannel, event);
        } else if (event.getChannelLeft() != null) {
            voiceChannel = event.getChannelLeft().asVoiceChannel();
            handleLeft(fleet, voiceChannel, event);
        }

        messageService.updatePanelEmbed(event.getJDA());
    }

    private void handleJoin(Fleet fleet, VoiceChannel voiceChannel, GuildVoiceUpdateEvent event) {
        Optional<QueuedUser> user = queueService.findQueuedUserById(event.getMember().getIdLong());

        if (user.isEmpty()) {
            return;
        }

        // Handle pending removal first
        if (user.get().getPendingRemoval() != null) {
            user.get().setPendingRemoval(null);
            queueService.updateQueuedUser(user.get());
            return;
        }

        // If they're not in a pending state for a channel, just return
        if (user.get().getPendingChannel() == null ||
                user.get().getPendingChannel() != voiceChannel.getIdLong()) {
            logger.info(
                    "Member {} joined channel {} without being in pending",
                    event.getMember().getId(),
                    voiceChannel.getId()
            );
            return;
        }

        // At this point, they are joining their pending channel
        // Only clear queue data if they're not in hop mode
        user.get().setPendingChannel(null);
        user.get().setPendingTimeout(null);
        user.get().setQueueNote(null);
        user.get().setQueueTime(null);
        user.get().setSentReminder(false);

        queueService.updateQueuedUser(user.get());

        fleet.getChannelShipInfo().get(voiceChannel.getIdLong())
                .addMember(event.getMember().getIdLong());
        fleetService.saveFleet(fleet);

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.JOIN.name());
        MDC.put("ship", voiceChannel.getName());
        MDC.put("user_id", event.getMember().getId());
        logger.info(
                "Member {} joined channel {} and was removed from pending",
                event.getMember().getId(),
                voiceChannel.getId()
        );
        MDC.clear();

        messageService.updatePanelEmbed(event.getJDA());
    }

    private void handleLeft(Fleet fleet, VoiceChannel voiceChannel, GuildVoiceUpdateEvent event) {
        Optional<QueuedUser> user = queueService.findQueuedUserById(event.getMember().getIdLong());

        // They left the channel
        // first checki f they are a member of the fleet or just a staff or random
        // person
        if (
            !fleet.getChannelShipInfo().get(voiceChannel.getIdLong()).getMemberIds()
                .contains(event.getMember().getIdLong())
        ) {
            logger.info(
                "Member {} left channel {} without being in fleet", event.getMember().getId(),
                voiceChannel.getId()
            );
            return;
        }

        if (user.isEmpty()) {
            return;
        }

        if (user.get().isHopMode()) {
            return;
        }

        // They are a member of the fleet
        // add them to the timer for 3m eviction
        user.get().setPendingRemoval(System.currentTimeMillis());
        queueService.updateQueuedUser(user.get());

        MDC.put("type", "fleet");
        MDC.put("fleet_type", FleetLogType.LEAVE_TIMEOUT_STARTED.name());
        MDC.put("ship", voiceChannel.getName());
        MDC.put("user_id", event.getMember().getId());
        logger.info(
            "Member {} left channel {} and was added to pending removal", event.getMember().getId(),
            voiceChannel.getId()
        );
        MDC.clear();

        messageService.updatePanelEmbed(event.getJDA());
    }
}
