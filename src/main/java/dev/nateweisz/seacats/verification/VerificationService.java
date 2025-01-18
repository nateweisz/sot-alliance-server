package dev.nateweisz.seacats.verification;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class VerificationService {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(VerificationService.class);

    private final BlacklistedUserRepository blacklistedUserRepository;
    private final OkHttpClient client = new OkHttpClient();

    @Value("${verification.user-token}")
    private String token;

    @Value("${verification.xbl-token}")
    private String xblToken;

    @Value("${jda.guildId}")
    private long guildId;

    public VerificationService(BlacklistedUserRepository blacklistedUserRepository) {
        this.blacklistedUserRepository = blacklistedUserRepository;
    }

    public CompletableFuture<VerificationResult> verify(long userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> connectedXboxGamertags = getConnectedXboxGamertags(userId);
                List<String> xboxIds = connectedXboxGamertags.stream().map(this::getXUIDFromGamerTag).filter(Objects::nonNull).toList();

                if (connectedXboxGamertags.isEmpty()) {
                    return new VerificationResult.NoAccountLinked();
                }

                for (String xboxId : xboxIds) {
                    if (blacklistedUserRepository.existsByxUid(Long.parseLong(xboxId))) {
                        return new VerificationResult.UserBlacklisted();
                    }
                }

                List<Pair<String, String>> blacklistedFriends = xboxIds.stream()
                        .map(this::getBlacklistedFriends)
                        .flatMap(List::stream)
                        .toList();

                if (blacklistedFriends.isEmpty()) {
                    return new VerificationResult.NothingFound(connectedXboxGamertags.getFirst());
                }

                return new VerificationResult.BlacklistedFriends(blacklistedFriends);
            } catch (IOException e) {
                LOGGER.error("Failed to verify user", e);
                return new VerificationResult.Failed(e);
            } catch (OutdatedAccountException e) {
                return new VerificationResult.OutdatedAccountLinked();
            }
        });
    }

    /**
     * Get the connected Xbox gamertags for a user
     *
     * @return List of connected Xbox gamertags ids
     */
    public List<String> getConnectedXboxGamertags(long userId) throws IOException {
        Request.Builder request = new Request.Builder()
                .url("https://discord.com/api/v9/users/%d/profile?with_mutual_guilds=true&with_mutual_friends=true&with_mutual_friends_count=false&guild_id=%d".formatted(userId, guildId))
                .header("Authorization", token);

        Call call = client.newCall(request.build());
        try (var response = call.execute()) {
            if (!response.isSuccessful()) {
                LOGGER.error("Failed to get connected accounts: %s".formatted(response.body().string()));
                throw new IOException("Failed to get connected accounts");
            }

            JSONObject json = new JSONObject(response.body().string());
            JSONArray connectedAccounts = json.getJSONArray("connected_accounts");

            return connectedAccounts.toList().stream()
                    .filter(account -> account instanceof Map)
                    .map(account -> (Map) account)
                    .filter(account -> "xbox".equals(account.get("type")) && Boolean.TRUE.equals(account.get("verified")))
                    .map(account -> (String) account.get("name"))
                    .collect(Collectors.toList());

        }
    }

    /**
     * @return A list of the blacklisted user's xuid and gamertag
     */
    private List<Pair<String, String>> getBlacklistedFriends(String xuid) {
        Request.Builder request = new Request.Builder()
                .url("https://xbl.io/api/v2/friends/%s".formatted(xuid))
                .header("x-authorization", xblToken);

        Call call = client.newCall(request.build());
        try (var response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get Xbox friends");
            }

            JSONObject json = new JSONObject(response.body().string());
            JSONArray friends = json.getJSONArray("people");

            return friends.toList().stream()
                    .map(friend -> {
                        if (friend instanceof Map) {
                            return new JSONObject((Map<?, ?>) friend);
                        }
                        return (JSONObject) friend;
                    })
                    .filter(friend -> {
                        try {
                            return blacklistedUserRepository.existsById(Long.parseLong(friend.getString("xuid")));
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid XUID format: %s".formatted(friend.getString("xuid")));
                            return false;
                        }
                    })
                    .map(friend -> Pair.of(friend.getString("xuid"), friend.getString("gamertag")))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Error fetching friends: %s".formatted(e.getMessage()));
            return List.of();
        }
    }

    public String getXUIDFromGamerTag(String gamerTag) {
        Request.Builder request = new Request.Builder()
                .url("https://xbl.io/api/v2/search/%s".formatted(gamerTag))
                .header("x-authorization", xblToken);

        Call call = client.newCall(request.build());
        try (var response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get XUID from gamertag");
            }

            JSONObject json = new JSONObject(response.body().string());
            JSONArray people = json.getJSONArray("people");
            if (people.isEmpty()) {
                throw new OutdatedAccountException("Xbox account outdated");
            }

            return ((JSONObject) (json.getJSONArray("people")
                    .get(0)))
                    .getString("xuid");
        } catch (IOException e) {
            return null;
        }
    }

    @Transactional
    public void deleteByDiscordId(String discordId) {
        blacklistedUserRepository.deleteAllByUserId(Long.parseLong(discordId));
    }
}
