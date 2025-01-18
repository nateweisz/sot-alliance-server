package dev.nateweisz.seacats.commands.resolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import dev.nateweisz.seacats.fleets.ShipType;

import io.github.freya022.botcommands.api.commands.application.slash.options.SlashCommandOption;
import io.github.freya022.botcommands.api.core.service.annotations.Resolver;
import io.github.freya022.botcommands.api.parameters.ClassParameterResolver;
import io.github.freya022.botcommands.api.parameters.Resolvers;
import io.github.freya022.botcommands.api.parameters.resolvers.SlashParameterResolver;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Resolver
public class ShipTypeResolver extends ClassParameterResolver<ShipTypeResolver, ShipType> implements SlashParameterResolver<ShipTypeResolver, ShipType> {

    public ShipTypeResolver() {
        super(ShipType.class);
    }

    @Override
    public @NotNull OptionType getOptionType() {
        return OptionType.STRING;
    }

    @NotNull
    @Override
    public Collection<Command.Choice> getPredefinedChoices(@Nullable Guild guild) {
        return Stream.of(ShipType.values())
            .map(u -> new Command.Choice(Resolvers.toHumanName(u), u.name()))
            .toList();
    }

    @Nullable
    @Override
    public ShipType resolve(
        @NotNull SlashCommandOption option,
        @NotNull CommandInteractionPayload event,
        @NotNull OptionMapping optionMapping
    ) {
        return Arrays.stream(ShipType.values()).filter(shipType -> shipType.getPretty().equalsIgnoreCase(optionMapping.getAsString())).findFirst().get();
    }
}
