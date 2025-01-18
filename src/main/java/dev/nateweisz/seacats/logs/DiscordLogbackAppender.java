package dev.nateweisz.seacats.logs;

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.github.freya022.botcommands.api.core.BContext;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DiscordLogbackAppender extends AppenderBase<ILoggingEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordLogbackAppender.class);

    private final LogFactory logFactory;
    private final LogConfigProperties logConfigProperties;
    private final BContext context;

    public DiscordLogbackAppender(
        LogFactory logFactory, LogConfigProperties logConfigProperties, BContext context
    ) {
        this.logFactory = logFactory;
        this.logConfigProperties = logConfigProperties;
        this.context = context;
    }

    @Override
    protected void append(ILoggingEvent event) {
        String logType = event.getMDCPropertyMap().get("type");

        if (logType == null) {
            return;
        }

        Log log = logFactory.createLog(logType, event.getMDCPropertyMap());

        if (log == null) {
            return;
        }

        Map<String, LogConfiguration> logging = logConfigProperties.getLogging();
        LogConfiguration logConfiguration = logging.get(logType);

        if (logConfiguration == null) {
            LOGGER.error("No configuration found for log type {}", logType);
            return;
        }

        postMessage(logConfiguration.webhookUrl(), log.createMessage());
    }

    private void postMessage(String webhookUrl, String message) {
        JDA jda = context.getService(JDA.class);

        net.dv8tion.jda.api.entities.WebhookClient.createClient(jda, webhookUrl)
            .sendMessage(message)
            .setAllowedMentions(List.of())
            .queue();
    }
}
