package dev.nateweisz.seacats.bot;

import java.util.Set;

import io.github.freya022.botcommands.api.core.JDAService;
import io.github.freya022.botcommands.api.core.events.BReadyEvent;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Bot extends JDAService {

    @Value("${jda.token}")
    private String token;

    @Override
    public @NotNull Set<net.dv8tion.jda.api.requests.GatewayIntent> getIntents() {
        return Set.of(GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_MEMBERS);
    }

    @Override
    public @NotNull Set<net.dv8tion.jda.api.utils.cache.CacheFlag> getCacheFlags() {
        return Set.of(CacheFlag.VOICE_STATE);
    }

    @Override
    protected void createJDA(
        @NotNull BReadyEvent bReadyEvent,
        @NotNull net.dv8tion.jda.api.hooks.IEventManager iEventManager
    ) {
        create(token)
            .setActivity(Activity.customStatus("Watching Scallywags"))
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .build();
    }
}
