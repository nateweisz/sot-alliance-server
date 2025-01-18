package dev.nateweisz.seacats.messages;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import dev.nateweisz.seacats.bot.EmbedService;

import dev.nateweisz.seacats.roster.StaffRoster;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final EmbedService embedService;
    private final StaffRoster staffRoster;

    @Value("${jda.guildId}")
    public long guildId;

    public MessageService(MessageRepository messageRepository, EmbedService embedService, StaffRoster staffRoster) {
        this.messageRepository = messageRepository;
        this.embedService = embedService;
        this.staffRoster = staffRoster;
    }

    @Scheduled(initialDelay = 1, fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    public void updateStaffRoster() {
        Optional<SavedMessage> staffRosterMessage = messageRepository.findByType(MessageType.STAFF_ROSTER);

        staffRosterMessage.ifPresent(
            savedMessage -> staffRoster.updateStaffRoster(guildId, savedMessage.getChannelId(), savedMessage.getId())
        );
    }

    /**
     * Updates the panel embed message if it has been posted.
     *
     * @param jda - the JDA instance to update the panel embed message with
     */
    public void updatePanelEmbed(JDA jda) {
        Guild guild = jda.getGuildById(guildId);
        Optional<SavedMessage> panelMessage = messageRepository.findByType(MessageType.FLEET_EMBED);

        panelMessage.ifPresent(
            savedMessage -> embedService.updateEmbed(
                guild, MessageType.FLEET_EMBED, savedMessage.getChannelId(), savedMessage.getId()
            )
        );
    }
}
