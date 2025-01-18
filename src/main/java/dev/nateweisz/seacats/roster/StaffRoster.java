package dev.nateweisz.seacats.roster;

import io.github.freya022.botcommands.api.core.BContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class StaffRoster {
    private final BContext context;

    @Value("#{'${jda.staff-roles}'.split(',')}")
    private List<Long> staffRoles;

    public StaffRoster(BContext context) {
        this.context = context;
    }

    public void updateStaffRoster(long guildId, long channelId, long messageId) {
        List<StaffRoleData> staffRoles = getStaffRoles(guildId);

        String text = """
                This is the roster of our staff team here at Sea Cats.
                
                %s
                """.formatted(staffRoles.stream()
                .map(role -> {
                    StringBuilder builder = new StringBuilder();
                    builder.append("<@&").append(role.roleId()).append("> **(%d)**:\n".formatted(role.staffMembers().size()));
                    role.staffMembers().forEach(memberId -> {
                        builder.append("<@").append(memberId).append(">\n");
                    });

                    return builder.toString();
                })
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse("No staff roles"));

        JDA jda = context.getService(JDA.class);
        Guild guild = jda.getGuildById(guildId);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Sea Cats Staff Roster")
                .setDescription(text)
                .setColor(0x00FF00)
                .setThumbnail(guild.getIconUrl());

        guild.getTextChannelById(channelId).editMessageEmbedsById(messageId, embed.build())
                .setContent(null)
                .setAllowedMentions(Collections.emptyList())
                .queue();
    }

    private List<StaffRoleData> getStaffRoles(long guildId) {
        JDA jda = context.getService(JDA.class);
        Guild guild = jda.getGuildById(guildId);

        Set<Long> processedUserIds = new HashSet<>();

        return staffRoles.stream()
                .map(guild::getRoleById)
                .filter(Objects::nonNull)
                .map(role -> {
                    List<Long> memberIds = guild.getMembersWithRoles(role).stream()
                            .filter(member -> !member.getUser().isBot())
                            .map(member -> member.getUser().getIdLong())
                            .filter(processedUserIds::add)
                            .toList();
                    return new StaffRoleData(role.getIdLong(), memberIds);
                })
                .toList();
    }
}
