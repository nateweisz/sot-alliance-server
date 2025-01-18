package dev.nateweisz.seacats.commands;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import dev.nateweisz.seacats.bot.EmbedService;
import dev.nateweisz.seacats.fleets.Fleet;
import dev.nateweisz.seacats.fleets.FleetService;
import dev.nateweisz.seacats.fleets.ShipInfo;
import dev.nateweisz.seacats.logs.QueueLogType;
import dev.nateweisz.seacats.messages.MessageService;
import dev.nateweisz.seacats.queue.QueueType;
import dev.nateweisz.seacats.queue.QueuedUser;
import dev.nateweisz.seacats.queue.V2QueuedService;

import dev.nateweisz.seacats.verification.VerificationResult;
import dev.nateweisz.seacats.verification.VerificationService;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import io.github.freya022.botcommands.api.components.annotations.ComponentTimeoutHandler;
import io.github.freya022.botcommands.api.components.annotations.JDAButtonListener;
import io.github.freya022.botcommands.api.components.data.ComponentTimeoutData;
import io.github.freya022.botcommands.api.components.event.ButtonEvent;
import io.github.freya022.botcommands.api.modals.Modals;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;

@Command
public class QueueCommands extends ApplicationCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueCommands.class);

    private final V2QueuedService queueService;
    private final FleetService fleetService;
    private final MessageService messageService;
    private final Modals modals;
    private final VerificationService verificationService;

    @Value("${jda.channels.fleet-inbox}")
    private long fleetInboxChannel;

    @Value("${jda.roles.verified}")
    private long verifiedRole;

    public QueueCommands(
            V2QueuedService queueService, FleetService fleetService, MessageService messageService,
            Modals modals, VerificationService verificationService
    ) {
        this.queueService = queueService;
        this.fleetService = fleetService;
        this.messageService = messageService;
        this.modals = modals;
        this.verificationService = verificationService;
    }

    @JDASlashCommand(
        name = "queue", subcommand = "toggle", description = "Toggles the queue open and closed"
    )
    @TopLevelSlashCommandData(description = "Manage the queue")
    public void toggleQueue(GuildSlashEvent event) {
        queueService.toggleQueue();
        event.reply("The queue is now " + (queueService.isQueueOpen() ? "open" : "closed") + ".")
            .setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());
    }

    @JDASlashCommand(
        name = "queue", subcommand = "process", description = "Processes a specific person in the queue"
    )
    public void processUser(
        GuildSlashEvent event,
        @SlashOption(name = "queued_user", description = "The user to process") Member user,
        @SlashOption(
            name = "ship_channel", description = "The voice channel of the ship to process them onto"
        ) VoiceChannel channel
    ) {
        Fleet fleet = fleetService.getFleetByCategory(channel.getParentCategoryIdLong());
        if (fleet == null) {
            event.reply("This channel is not a ship channel.").setEphemeral(true).queue();
            return;
        }

        ShipInfo info = fleet.getChannelShipInfo().get(channel.getIdLong());

        if (info == null) {
            event.reply("This channel is not a ship channel.").setEphemeral(true).queue();
            return;
        }

        if (!queueService.isQueueOpen()) {
            event.reply(
                "⚠️ The queue is currently closed, so you can't pounce in just yet. Our fur-midable staff will meow at the @Fleet Alert role when the queue opens up! \uD83D\uDC3E"
            ).setEphemeral(true).queue();
            return;
        }

        Optional<QueuedUser> queuedUser = queueService.findQueuedUserById(user.getIdLong());
        if (queuedUser.isEmpty() || !queuedUser.get().isInQueue()) {
            event.reply("⚠️ This user is not in the queue.").setEphemeral(true).queue();
            return;
        }

        /*
         * Steps of processing a user: Dm them with the info from being processed
         */

        channel.createInvite()
            .setMaxUses(1)
            // .deadline(120)
            .queue(invite -> {
                user.getUser().openPrivateChannel()
                    .queue(dm -> {
                        dm.sendMessage(
                            """
                                <:ArtieCat:1247001608748007494> A cozy spot on **F%s, Ship %d** has been found for you! You have **3 minutes** to pounce into the channel before this fur-tastic invite link expires: %s 🐾

                                <:seaCatsCoins:1306419566968176720> Love what we do in the Sea Cats' crew? Show your sup-pawt by becoming a patron and enjoy exclusive Discord paw-sitions, titles, and other clawsome benefits! <https://patreon.com/seacatsscallywags>
                                ## :warning: NEVER ACCEPT PAW-SOME INVITES OR FRIEND REQUESTS FROM ANYONE OUTSIDE YOUR VOICE CHANNEL, EVEN IF THEY CLAIM TO BE STAFF! 🐾 \s
                                **Purr-tip**: If the "Join Voice" button isn't working, try joining the **F%s, Ship %d** channel directly, or use this paw-some backup link: <https://discord.com/channels/%d/%d> \s
                                """
                                .formatted(
                                    fleet.getNumericalId(), info.getShipId(), invite.getUrl(),
                                    fleet.getNumericalId(), info.getShipId(),
                                    event.getGuild().getIdLong(), channel.getIdLong()
                                )
                        )
                            .queue();

                        queuedUser.get().setPendingChannel(channel.getIdLong());
                        queuedUser.get().setPendingTimeout(System.currentTimeMillis());
                        queueService.updateQueuedUser(queuedUser.get());

                        event.getGuild()
                            .addRoleToMember(user, event.getGuild().getRoleById(fleet.getRoleId()))
                            .queue();
                        event.getGuild()
                            .addRoleToMember(user, event.getGuild().getRoleById(info.getRoleId()))
                            .queue();
                        event.reply("User has been processed and DM'd.").setEphemeral(true).queue();

                        messageService.updatePanelEmbed(event.getJDA());

                        MDC.put("type", "queue");
                        MDC.put("queue_type", QueueLogType.PROCESS.name());
                        MDC.put("user_id", String.valueOf(user.getIdLong()));
                        MDC.put("staff_id", String.valueOf(event.getMember().getIdLong()));
                        MDC.put("ship", "F%s, Ship %d".formatted(fleet.getNumericalId(), info.getShipId()));

                        LOGGER
                            .info("User has been processed: %s".formatted(user.getEffectiveName()));
                        MDC.clear();
                    }, failure -> {
                        event.reply("User does not have DMs enabled and cannot be processed.")
                            .setEphemeral(true).queue();
                    });
            }, failure -> {
                event.reply("Failed to create invite for user.").setEphemeral(true).queue();
            });
    }

    @JDASlashCommand(
        name = "queue", subcommand = "remove", description = "Removes a user from the queue"
    )
    public void removeUser(
        GuildSlashEvent event,
        @SlashOption(name = "queued_user", description = "The user to remove") QueuedUser user,
        @SlashOption(name = "reason", description = "The reason for removal") String reason
    ) {
        event.reply("User has been removed from the queue.").setEphemeral(true).queue();

        user.setQueueTime(null);
        user.setQueueNote(null);
        user.setStaffNote(null);
        user.setType(null);
        queueService.updateQueuedUser(user);
        messageService.updatePanelEmbed(event.getJDA());

        event.getJDA().openPrivateChannelById(user.getMemberId())
            .queue(dm -> {
                dm.sendMessage(
                    "<:AwesomeCat:1246996884632109178> Ahoy, meow-ty! You've been paw-sitively booted from the queue for the following reason: %s"
                        .formatted(reason)
                ).queue();
            }, failure -> {
                event.reply("User does not have DMs enabled and cannot be notified.")
                    .setEphemeral(true).queue();
            });
    }

    @JDASlashCommand(
        name = "queue", subcommand = "add-note", description = "Adds a staff note to a user in the queue"
    )
    public void addNote(
        GuildSlashEvent event,
        @SlashOption(
            name = "queued_user", description = "The user to add a note to"
        ) QueuedUser user,
        @SlashOption(name = "note", description = "The note to add") String note
    ) {
        event.reply("Note has been added to the user.").setEphemeral(true).queue();

        user.setStaffNote(note);
        queueService.updateQueuedUser(user);

        messageService.updatePanelEmbed(event.getJDA());
    }

    @JDASlashCommand(
        name = "queue", subcommand = "remove-note", description = "Removes a note from a user in the queue"
    )
    public void removeNote(
        GuildSlashEvent event,
        @SlashOption(
            name = "queued_user", description = "The user to remove a note from"
        ) QueuedUser user
    ) {
        event.reply("Note has been removed from the user.").setEphemeral(true).queue();

        user.setStaffNote(null);
        queueService.updateQueuedUser(user);

        messageService.updatePanelEmbed(event.getJDA());
    }

    @JDAButtonListener(EmbedService.QUEUE_JOIN_UPDATE)
    public void joinQueue(ButtonEvent event) {
        if (!queueService.isQueueOpen()) {
            event.reply(
                "⚠️ The queue is currently closed, so you can't pounce in just yet. Our fur-midable staff will meow at the @Fleet Alert role when the queue opens up! \uD83D\uDC3E"
            ).setEphemeral(true).queue();
            return;
        }

        var modal =
            modals.create("Join Fleet Queue")
                .bindTo(modalEvent -> {
                    var memberId = event.getMember().getIdLong();
                    var activity = modalEvent.getValues().getFirst().getAsString();

                    if (
                        event.getMember().getRoles().stream().map(ISnowflake::getIdLong)
                            .noneMatch(id -> id == verifiedRole)
                    ) {
                        modalEvent.reply(
                            "Remember, you must be voice verified to join the queue. :smiley_cat:"
                        ).setEphemeral(true).queue();
                        return;
                    }

                    Optional<QueuedUser> user = queueService.findQueuedUserById(memberId);
                    if (user.isPresent() && user.get().isInQueue()) {
                        user.get().setQueueNote(activity);
                        queueService.updateQueuedUser(user.get());

                        modalEvent.reply("\uD83D\uDC3E Purr-fect! Your activity has been updated.")
                            .setEphemeral(true).queue();

                        MDC.put("type", "queue");
                        MDC.put("queue_type", QueueLogType.UPDATE_NOTE.name());
                        MDC.put("user_id", String.valueOf(memberId));
                        MDC.put("note", activity);
                        LOGGER.info(
                            "User has updated their queue note: %s"
                                .formatted(event.getMember().getEffectiveName())
                        );
                        MDC.clear();

                        messageService.updatePanelEmbed(modalEvent.getJDA());
                    } else {
                        // Do verification checks here
                        modalEvent.deferReply(true).queue();
                        modalEvent.getHook().editOriginal(":alarm_clock: We are currently verifying you against our database. Please wait a moment.").queue();

                        verificationService.verify(modalEvent.getMember().getIdLong())
                                        .thenAccept(result -> {
                                            if (result instanceof VerificationResult.UserBlacklisted) {
                                                modalEvent.getHook().editOriginal("Sorry, you are blacklisted and not welcome here.").queue();
                                                return;
                                            }

                                            if (result instanceof VerificationResult.NoAccountLinked) {
                                                modalEvent.getHook().editOriginal("You need to link an Xbox account to your discord.").queue();
                                                return;
                                            }

                                            if (result instanceof VerificationResult.OutdatedAccountLinked) {
                                                modalEvent.getHook().editOriginal("The Xbox account you have linked is has an outdated gamertag. Please re-link it to your discord before continuing.").queue();
                                                return;
                                            }

                                            if (result instanceof VerificationResult.BlacklistedFriends blacklistedFriends) {
                                                modalEvent.getHook().editOriginal(":x: You have not passed the verification check. You have received a DM with more information.").queue();

                                                modalEvent.getMember().getUser().openPrivateChannel().queue(
                                                        dm -> {
                                                            dm.sendMessage(
                                                                    """
                                                                        :x: You have been flagged as having blacklisted friends on Xbox Live. Please remove these friends and try again. If you believe this is in error, please contact staff.
                                                                        
                                                                        `%s`
                                                                        """.formatted(
                                                                            String.join(
                                                                                    ", ",
                                                                                    blacklistedFriends.blacklistedFriends().stream()
                                                                                            .map(Pair::getSecond)
                                                                                            .toList()
                                                                            )
                                                                    )
                                                            ).queue();
                                                        });
                                                return;
                                            }

                                            if (result instanceof VerificationResult.Failed(Exception cause)) {
                                                modalEvent.getHook().editOriginal("Something went wrong while verifying you, contact an admin for more information.").queue();
                                                LOGGER.error("Failed to verify account", cause);
                                            }

                                            if (user.isPresent()) {
                                                user.get().setQueueTime(System.currentTimeMillis());
                                                user.get().setQueueNote(activity);
                                                user.get().setType(QueueType.NORMAL);
                                                user.get().setSentReminder(false);
                                                queueService.updateQueuedUser(user.get());
                                            } else {
                                                queueService.joinQueue(memberId, QueueType.NORMAL, activity);
                                            }

                                            modalEvent.getHook().editOriginal(
                                                    "\uD83D\uDC3E Pawsome! You've successfully joined the queue! You'll be visible in the queue list shortly after it updates."
                                            ).queue();

                                            MDC.put("type", "queue");
                                            MDC.put("queue_type", QueueLogType.JOIN.name());
                                            MDC.put("user_id", String.valueOf(memberId));
                                            MDC.put("note", activity);
                                            LOGGER.info(
                                                    "User has joined the queue: %s"
                                                            .formatted(event.getMember().getEffectiveName())
                                            );
                                            MDC.clear();

                                            messageService.updatePanelEmbed(modalEvent.getJDA());
                                        })
                                        .exceptionally(e -> {
                                            e.printStackTrace();
                                            modalEvent.getHook().editOriginal(":x: An error occurred while verifying you. Contact staff ASAP to get this resolved.").queue();
                                            return null;
                                        });
                    }
                })
                .addComponents(
                    ActionRow.of(
                        modals
                            .createTextInput(
                                "Type Your Desired Activity", "activity", TextInputStyle.PARAGRAPH
                            )
                            .setRequired(true)
                            .setRequiredRange(0, 100)
                            .setPlaceholder(
                                "You can change this at any time later using the button without losing your place in queue."
                            )
                            .build()
                    )
                )
                .build();

        event.replyModal(modal).queue();
    }

    @JDASlashCommand(
        name = "queue", subcommand = "priority-for-channel", description = "Gives all user's in a channel priority in the queue"
    )
    public void priorityForChannel(
        GuildSlashEvent event,
        @SlashOption(
            name = "channel", description = "The voice channel to give priority to"
        ) VoiceChannel channel,
        @SlashOption(name = "note", description = "The note to add to the queue") String note
    ) {
        channel.getMembers().forEach(member -> {
            Optional<QueuedUser> user = queueService.findQueuedUserById(member.getIdLong());
            if (user.isPresent()) {
                user.get().setType(QueueType.PRIORITY);
                user.get().setPriorityReason(note);
                queueService.updateQueuedUser(user.get());
            }
        });

        event.reply("All users in the channel have been given priority in the queue.")
            .setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());
    }

    @JDASlashCommand(
        name = "queue", subcommand = "priority-for-user", description = "Gives a user priority in the queue"
    )
    public void priorityForUser(
        GuildSlashEvent event,
        @SlashOption(
            name = "user", description = "The user to give priority to"
        ) Member user,
        @SlashOption(name = "note", description = "The note to add to the queue") String note
    ) {
        if (!queueService.isQueueOpen()) {
            event.reply(
                "⚠️ The queue is currently closed, so you can't pounce in just yet. Our fur-midable staff will meow at the @Fleet Alert role when the queue opens up! \uD83D\uDC3E"
            ).setEphemeral(true).queue();
            return;
        }

        Optional<QueuedUser> queuedUser = queueService.findQueuedUserById(user.getIdLong());
        if (queuedUser.isEmpty() || !queuedUser.get().isInQueue()) {
            event.reply("⚠️ This user is not in the queue.").setEphemeral(true).queue();
            return;
        }

        queuedUser.get().setType(QueueType.PRIORITY);
        queuedUser.get().setPriorityReason(note);
        queueService.updateQueuedUser(queuedUser.get());

        event.reply("User has been given priority in the queue.").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());
    }

    @JDAButtonListener(EmbedService.QUEUE_LEAVE)
    public void leaveQueue(ButtonEvent event) {
        var memberId = event.getMember().getIdLong();

        Optional<QueuedUser> user = queueService.findQueuedUserById(memberId);
        if (user.isEmpty() || !user.get().isInQueue()) {
            event.reply("You are not in the queue.").setEphemeral(true).queue();
            return;
        }

        event.reply("You have been removed from the queue.").setEphemeral(true).queue();
        user.get().setQueueTime(null);
        user.get().setQueueNote(null);
        user.get().setStaffNote(null);
        user.get().setType(null);
        queueService.updateQueuedUser(user.get());

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "queue");
        MDC.put("queue_type", QueueLogType.LEAVE.name());
        MDC.put("user_id", String.valueOf(memberId));
        LOGGER.info("User has left the queue: %s".formatted(event.getMember().getEffectiveName()));
        MDC.clear();
    }

    @JDAButtonListener(EmbedService.CONTACT_STAFF)
    public void contactStaff(ButtonEvent event) {
        var modal =
            modals.create("Contact Fleet Staff")
                .bindTo(modalEvent -> {
                    var message = modalEvent.getValues().getFirst().getAsString();
                    modalEvent.getGuild().getTextChannelById(fleetInboxChannel).sendMessageEmbeds(
                        new EmbedBuilder()
                            .setTitle("Fleet Contact Request")
                            .setDescription("`%s`".formatted(message))
                            .setFooter(
                                "Requested by %s".formatted(event.getMember().getEffectiveName()),
                                event.getMember().getUser().getAvatarUrl()
                            )
                            .setTimestamp(Instant.now())
                            .build()
                    ).queue();

                    modalEvent.reply("Your message has been sent to the fleet staff.")
                        .setEphemeral(true).queue();
                })
                .addComponents(
                    ActionRow.of(
                        modals.createTextInput("Message", "message", TextInputStyle.PARAGRAPH)
                            .setRequired(true)
                            .setRequiredRange(0, 256)
                            .setPlaceholder("Please provide a brief description of your issue.")
                            .build()
                    )
                )
                .build();

        event.replyModal(modal).queue();
    }

    @JDAButtonListener("remainInQueueButton")
    public void remainInQueueButton(ButtonEvent event) {
        Optional<QueuedUser> user = queueService.findQueuedUserById(event.getUser().getIdLong());
        if (user.isEmpty() || user.get().isAfkTimeoutOver()) {
            return;
        }

        user.get().setAfkTimeout(null);
        queueService.updateQueuedUser(user.get());

        event.getMessage().editMessage("You have chosen to remain in the queue.").queue();
        event.reply("You have chosen to remain in the queue.").setEphemeral(true).queue();
    }

    @JDAButtonListener("leaveQueueButton")
    public void leaveQueueButton(ButtonEvent event) {
        var memberId = event.getMember().getIdLong();

        Optional<QueuedUser> user = queueService.findQueuedUserById(memberId);
        if (user.isEmpty() || !user.get().isInQueue()) {
            event.reply("You are not in the queue.").setEphemeral(true).queue();
            return;
        }

        event.reply("You have been removed from the queue.").setEphemeral(true).queue();
        user.get().setQueueTime(null);
        user.get().setQueueNote(null);
        user.get().setStaffNote(null);
        user.get().setType(null);
        user.get().setAfkTimeout(null);
        queueService.updateQueuedUser(user.get());

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "queue");
        MDC.put("queue_type", QueueLogType.LEAVE.name());
        MDC.put("user_id", String.valueOf(memberId));
        LOGGER.info("User has left the queue: %s".formatted(event.getMember().getEffectiveName()));
        MDC.clear();
    }
}
