package dev.nateweisz.seacats.commands.resolver;

import java.util.Optional;

import dev.nateweisz.seacats.queue.QueuedUser;
import dev.nateweisz.seacats.queue.V2QueuedService;

import io.github.freya022.botcommands.api.commands.application.slash.options.SlashCommandOption;
import io.github.freya022.botcommands.api.core.service.annotations.Resolver;
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver;
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Resolver
public class QueuedUserResolver extends ClassParameterResolver<QueuedUserResolver, QueuedUser> implements SlashParameterResolver<QueuedUserResolver, QueuedUser> {
    private final V2QueuedService queueService;

    public QueuedUserResolver(V2QueuedService queueService) {
        super(QueuedUser.class);
        this.queueService = queueService;
    }

    @Override
    public @NotNull OptionType getOptionType() {
        return OptionType.USER;
    }

    @Override
    public @Nullable QueuedUser resolve(
        @NotNull SlashCommandOption option,
        @NotNull CommandInteractionPayload event,
        @NotNull OptionMapping optionMapping
    ) {
        long memberId = optionMapping.getAsLong();
        Optional<QueuedUser> queuedUser = queueService.findQueuedUserById(memberId);

        if (queuedUser.isEmpty() && event instanceof SlashCommandInteractionEvent) {
            ((SlashCommandInteractionEvent) event).reply("⚠ This user is not in the queue.")
                .setEphemeral(true).queue();
            return null;
        }

        return queuedUser.orElse(null);
    }
}
