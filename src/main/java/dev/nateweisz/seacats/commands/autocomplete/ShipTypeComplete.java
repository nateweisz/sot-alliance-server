package dev.nateweisz.seacats.commands.autocomplete;

import java.util.Arrays;
import java.util.Collection;

import dev.nateweisz.seacats.fleets.ShipType;

import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler;
import io.github.freya022.botcommands.api.core.annotations.Handler;
import io.github.freya022.botcommands.api.parameters.Resolvers;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

@Handler
public class ShipTypeComplete extends ApplicationCommand {
    public static final String SHIP_TYPE_COMPLETE = "Ship Type Complete";

    @AutocompleteHandler(SHIP_TYPE_COMPLETE)
    public Collection<String> onComplete(CommandAutoCompleteInteractionEvent event) {
        return Arrays.stream(ShipType.values()).map(Resolvers::toHumanName).toList();
    }
}
