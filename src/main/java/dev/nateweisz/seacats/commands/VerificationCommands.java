package dev.nateweisz.seacats.commands;

import dev.nateweisz.seacats.verification.BlacklistedUser;
import dev.nateweisz.seacats.verification.BlacklistedUserRepository;
import dev.nateweisz.seacats.verification.VerificationResult;
import dev.nateweisz.seacats.verification.VerificationService;
import io.github.freya022.botcommands.api.commands.annotations.Command;
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand;
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand;
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption;
import net.dv8tion.jda.api.entities.Member;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Command
public class VerificationCommands extends ApplicationCommand {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(VerificationCommands.class);

    private final VerificationService verificationService;
    private final BlacklistedUserRepository blacklistedUserRepository;

    @Value("${jda.roles.voice-verified}")
    private long voiceVerifiedRoleId;

    @Value("${jda.roles.verified}")
    private long verifiedRoleId;

    @Value("${jda.roles.pawsome}")
    private long pawsomeRoleId;

    public VerificationCommands(VerificationService verificationService, BlacklistedUserRepository blacklistedUserRepository) {
        this.verificationService = verificationService;
        this.blacklistedUserRepository = blacklistedUserRepository;
    }

    @JDASlashCommand(
            name = "verify", description = "Verify a user and check that they have no blacklisted xbox friends"
    )
    public void verify(
            GuildSlashEvent event,
            @SlashOption(name = "user", description = "The user to verify") Member user
    ) {
        event.deferReply(true).queue();

        verificationService.verify(user.getIdLong())
                        .thenAccept(verified -> {
                            if (verified instanceof VerificationResult.NothingFound(String linkedAccount)) {
                                event.getMember().getGuild().addRoleToMember(user, event.getMember().getGuild().getRoleById(verifiedRoleId)).queue();
                                event.getMember().getGuild().addRoleToMember(user, event.getMember().getGuild().getRoleById(pawsomeRoleId)).queue();
                                event.getMember().getGuild().modifyNickname(user, linkedAccount).queue();
                                event.getHook().editOriginal("User has been verified").queue();
                            } else if (verified instanceof VerificationResult.BlacklistedFriends){
                                event.getHook().editOriginal("User has blacklisted friends").queue();
                            } else if (verified instanceof VerificationResult.OutdatedAccountLinked) {
                                event.getHook().editOriginal("User has outdated account").queue();
                            } else if (verified instanceof VerificationResult.NoAccountLinked) {
                                event.getHook().editOriginal("User has no account linked").queue();
                            } else if (verified instanceof VerificationResult.UserBlacklisted) {
                                event.getHook().editOriginal("User is blacklisted").queue();
                            } else if (verified instanceof VerificationResult.Failed(Exception cause)) {
                                event.getHook().editOriginal("Failed to verify user.").queue();
                                LOGGER.error("Failed to verify user", cause);
                            }
                        })
                        .exceptionally(e -> {
                            event.getHook().editOriginal("Something went wrong while verifying user.").queue();
                            LOGGER.error("Failed to verify user", e);
                            return null;
                        });
    }

    @JDASlashCommand(
            name = "voice-verify", description = "Verify a user for voice chat"
    )
    public void voiceVerify(
            GuildSlashEvent event,
            @SlashOption(name = "user", description = "The user to verify") Member user
    ) {
        event.getMember().getGuild().addRoleToMember(user, event.getMember().getGuild().getRoleById(voiceVerifiedRoleId)).queue();
        event.reply("User has been voice verified").setEphemeral(true).queue();
    }

    @JDASlashCommand(
            name = "blacklist", description = "Blacklist a user"
    )
    public void blacklist(
            GuildSlashEvent event,
            @SlashOption(name = "user", description = "The user to blacklist") Member user,
            @SlashOption(name = "reason", description = "The reason for blacklisting the user") String reason
    ) {

        if (blacklistedUserRepository.existsByUserId(user.getIdLong()))
            event.reply("User is already blacklisted").setEphemeral(true).queue();
        else {
            event.deferReply(true).queue();
            try {
                List<String> connectedXboxGamertags = verificationService.getConnectedXboxGamertags(user.getIdLong());
                List<String> connectedXuids = connectedXboxGamertags.stream()
                        .map(verificationService::getXUIDFromGamerTag)
                        .toList();

                int i = 0;
                for (String xuid : connectedXuids) {
                    LOGGER.info("Blacklisting user %s with discord id %d".formatted(xuid, user.getIdLong()));
                    blacklistedUserRepository.save(new BlacklistedUser(Long.parseLong(xuid), user.getIdLong(), System.currentTimeMillis(), event.getUser().getIdLong(), reason, connectedXboxGamertags.get(i++)));
                }

                event.getHook().editOriginal("User has been blacklisted").queue();
            } catch (IOException e) {
                event.getHook().editOriginal("An error occurred while blacklisting the user").queue();
                throw new RuntimeException(e);
            }
        }
    }

    @JDASlashCommand(
            name = "blacklists", description = "Get a list of blacklisted users"
    )
    public void blacklists(GuildSlashEvent event) {
        List<BlacklistedUser> blacklistedUsers = blacklistedUserRepository.findAll();
        event.reply("Blacklisted users: \n" + blacklistedUsers.stream()
                .map(user -> "- %s (Reason `%s`) at <t:%d:f> by <@%d>".formatted(user.getGamerTag(), user.getReason(), user.getBlacklistedAt() / 1000, user.getBlacklistedBy()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("No blacklisted users found")
        ).setEphemeral(true).queue();
    }

    @JDASlashCommand(
            name = "unblacklist", description = "Unblacklist a user"
    )
    public void unBlacklist(
            GuildSlashEvent event,
            @SlashOption(name = "user", description = "The user to unblacklist") Member user
    ) {
        if (blacklistedUserRepository.existsByUserId(user.getIdLong())) {
            verificationService.deleteByDiscordId(user.getId());
            event.reply("User has been unblacklisted.").setEphemeral(true).queue();
        } else {
            event.reply("User is not blacklisted").setEphemeral(true).queue();
        }
    }

    @JDASlashCommand(
            name = "blacklist-user", description = "Blacklist a user based on their gamer tag"
    )
    public void blacklistUser(
            GuildSlashEvent event,
            @SlashOption(name = "gamertag", description = "The gamertag to blacklist") String gamertag,
            @SlashOption(name = "reason", description = "The reason for blacklisting the user") String reason
    ) {
        event.deferReply(true).queue();
        String xuid = verificationService.getXUIDFromGamerTag(gamertag);

        if (xuid == null) {
            event.getHook().editOriginal("User not found").queue();
            return;
        }

        if (blacklistedUserRepository.existsByxUid(Long.parseLong(xuid))) {
            event.getHook().editOriginal("User is already blacklisted").queue();
        } else {
            LOGGER.info("Blacklisting user %s with gamertag %s".formatted(xuid, gamertag));
            blacklistedUserRepository.save(
                    new BlacklistedUser(Long.parseLong(xuid), 0, System.currentTimeMillis(), event.getUser().getIdLong(), reason, gamertag));
            event.getHook().editOriginal("User has been blacklisted").queue();
        }
    }
}
