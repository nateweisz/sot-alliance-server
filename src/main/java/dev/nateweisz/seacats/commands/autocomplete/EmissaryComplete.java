package dev.nateweisz.seacats.commands.autocomplete;

import java.util.Collection;
import java.util.List;

import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler;
import io.github.freya022.botcommands.api.core.annotations.Handler;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

@Handler
public class EmissaryComplete extends ApplicationCommand {
    public static final String EMISSARY_COMPLETE = "Emissary Complete";

    private final String[] EMISSARIES =
        {
            "RB.E",
            "A.E",
            "GH.E",
            "M.E",
            "OOS.E",
            "SOV.E",
            "FLEX.E"
        };

    @AutocompleteHandler(EMISSARY_COMPLETE)
    public Collection<String> onComplete(CommandAutoCompleteInteractionEvent event) {
        return List.of(EMISSARIES);
    }
}
