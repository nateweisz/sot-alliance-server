package dev.nateweisz.seacats.commands;

import dev.nateweisz.seacats.bot.EmbedService;
import dev.nateweisz.seacats.messages.MessageRepository;
import dev.nateweisz.seacats.messages.MessageType;
import dev.nateweisz.seacats.messages.SavedMessage;

import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import net.dv8tion.jda.api.Permission;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Command
public class PanelCommand extends ApplicationCommand {
    private final EmbedService embedService;
    private final MessageRepository messageRepository;
    private final TransactionTemplate transactionTemplate;

    public PanelCommand(
        EmbedService embedService,
        MessageRepository messageRepository,
        TransactionTemplate transactionTemplate
    ) {
        this.embedService = embedService;
        this.messageRepository = messageRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @JDASlashCommand(
        name = "panel", description = "Posts the panel for the fleets, should only need to be done once but overrides previous one"
    )
    public void panel(GuildSlashEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You do not have permission to use this command").setEphemeral(true).queue();
            return;
        }

        var channel = event.getGuildChannel().asTextChannel();
        channel.sendMessage(".").queue(callback -> {
            var lastMessage = messageRepository.findByType(MessageType.FLEET_EMBED);
            if (lastMessage.isPresent()) {
                event.getGuild().getTextChannelById(lastMessage.get().getChannelId())
                    .deleteMessageById(lastMessage.get().getId()).queue();

                transactionTemplate.execute((TransactionCallback<Void>) status -> {
                    messageRepository.deleteByType(MessageType.FLEET_EMBED);
                    return null;
                });
            }

            messageRepository.save(
                new SavedMessage(
                    callback.getIdLong(),
                    callback.getChannelIdLong(),
                    MessageType.FLEET_EMBED
                )
            );
            embedService.updateEmbed(
                event.getGuild(), MessageType.FLEET_EMBED, callback.getChannelIdLong(),
                callback.getIdLong()
            );
        });
    }
}
