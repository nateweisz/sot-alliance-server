package dev.nateweisz.seacats.commands.autocomplete;

import java.util.Collection;
import java.util.List;

import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler;
import io.github.freya022.botcommands.api.core.annotations.Handler;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;

@Handler
public class ActivityComplete extends ApplicationCommand {
    public static final String ACTIVITY_COMPLETE = "Activity Complete";

    private final String[] ACTIVITIES =
        {
            "World Events",
            "FOTD",
            "Skeleton Camps",
            "Gilded Voyages",
            "Medleys",
            "LOTV",
            "Voyage of Legends",
            "Buried Treasure",
            "Riddles",
            "Animals",
            "Lost Shipments",
            "Bounties",
            "Ghost Ships",
            "Skeleton Lords",
            "Sea Forts",
            "Sunken Kingdom",
            "Vaults",
            "Cargo",
            "The Shroudbreaker",
            "The Cursed Rogue",
            "The Legendary Storyteller",
            "Stars of a Thief",
            "Wild Rose",
            "The Art of the Trickster",
            "The Fate of the Morningstar",
            "Revenge of the Morningstar",
            "Shores of Gold",
            "The Seabound Soul",
            "Heart of Fire",
            "Pirate's Life 2: The Sunken Pearl",
            "Pirate's Life 4. Dark Brethren",
            "Pirate's Life 5. Lords of the Sea",
            "Meg Hunt",
            "Fishing",
        };

    @AutocompleteHandler(ACTIVITY_COMPLETE)
    public Collection<String> onComplete(CommandAutoCompleteInteractionEvent event) {
        return List.of(ACTIVITIES);
    }
}
