package dev.nateweisz.seacats.logs;

import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jda")
@Data
public class LogConfigProperties {
    private Map<String, LogConfiguration> logging;
}
