package dev.nateweisz.seacats.commands;

import dev.nateweisz.seacats.logs.StaffLogType;
import dev.nateweisz.seacats.messages.MessageRepository;
import dev.nateweisz.seacats.messages.MessageService;
import dev.nateweisz.seacats.messages.MessageType;
import dev.nateweisz.seacats.messages.SavedMessage;
import dev.nateweisz.seacats.queue.V2QueuedService;

import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

@Command
public class DutyCommands extends ApplicationCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DutyCommands.class);

    private final V2QueuedService queueService;
    private final MessageService messageService;
    private final MessageRepository messageRepository;
    private final TransactionTemplate transactionTemplate;

    public DutyCommands(V2QueuedService queueService, MessageService messageService, MessageRepository messageRepository, TransactionTemplate transactionTemplate) {
        this.queueService = queueService;
        this.messageService = messageService;
        this.messageRepository = messageRepository;
        this.transactionTemplate = transactionTemplate;

        LOGGER.info("DutyCommands initialized");
    }

    @JDASlashCommand(name = "staff", subcommand = "sign-on", description = "Toggle duty status")
    @TopLevelSlashCommandData(description = "Staff commands")
    public void signOn(
        GuildSlashEvent event
    ) {
        if (queueService.findAllOnDutyUsers().contains(event.getMember().getIdLong())) {
            event.reply("You are already on duty").setEphemeral(true).queue();
            return;
        }

        queueService.addOnDutyUser(event.getMember().getIdLong());
        event.reply("You are now on duty").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "staff");
        MDC.put("staff_type", StaffLogType.ON_DUTY.name());
        MDC.put("staff_id", event.getMember().getId());
        LOGGER.info("Staff %s is now on duty".formatted(event.getMember().getEffectiveName()));
        MDC.clear();
    }

    @JDASlashCommand(name = "staff", subcommand = "sign-off", description = "Toggle duty status")
    public void signOff(
        GuildSlashEvent event
    ) {
        if (!queueService.findAllOnDutyUsers().contains(event.getMember().getIdLong())) {
            event.reply("You are already off duty").setEphemeral(true).queue();
            return;
        }

        queueService.removeOnDutyUser(event.getMember().getIdLong());
        event.reply("You are now off duty").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());
    }

    @JDASlashCommand(
        name = "staff", subcommand = "remove", description = "Remove a staff member from duty"
    )
    public void remove(
        GuildSlashEvent event,
        @SlashOption(name = "member", description = "The staff member to remove") Member member
    ) {
        if (!queueService.findAllOnDutyUsers().contains(member.getIdLong())) {
            event.reply("Staff member is already off duty").setEphemeral(true).queue();
            return;
        }

        queueService.removeOnDutyUser(member.getIdLong());
        event.reply("Staff member removed from duty").setEphemeral(true).queue();

        messageService.updatePanelEmbed(event.getJDA());

        MDC.put("type", "staff");
        MDC.put("staff_type", StaffLogType.OFF_DUTY.name());
        MDC.put("staff_id", member.getId());
        LOGGER.info("Staff %s is now off duty".formatted(member.getEffectiveName()));
        MDC.clear();
    }

    @JDASlashCommand(
            name = "staff", subcommand = "panel", description = "Update the duty panel"
    )
    public void panel(
            GuildSlashEvent event
    ) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You do not have permission to update the panel").setEphemeral(true).queue();
            return;
        }

        var channel = event.getGuildChannel().asTextChannel();
        channel.sendMessage(".").queue(callback -> {
            var lastMessage = messageRepository.findByType(MessageType.STAFF_ROSTER);
            if (lastMessage.isPresent()) {
                event.getGuild().getTextChannelById(lastMessage.get().getChannelId())
                        .deleteMessageById(lastMessage.get().getId()).queue();

                transactionTemplate.execute((TransactionCallback<Void>) status -> {
                    messageRepository.deleteByType(MessageType.STAFF_ROSTER);
                    return null;
                });
            }

            messageRepository.save(
                    new SavedMessage(
                            callback.getIdLong(),
                            callback.getChannelIdLong(),
                            MessageType.STAFF_ROSTER
                    )
            );

            messageService.updatePanelEmbed(event.getJDA());
        });
    }
}
