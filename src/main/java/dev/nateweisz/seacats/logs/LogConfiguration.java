package dev.nateweisz.seacats.logs;

import java.util.Optional;

/**
 * Configuration for the Discord webhook logger
 *
 * @param webhookUrl The URL of the webhook to send logs to
 * @param username   - Optional username to use for the webhook
 * @param avatarUrl  - Optional avatar URL to use for the webhook
 */
public record LogConfiguration(
    String webhookUrl,
    Optional<String> username,
    Optional<String> avatarUrl
) {}
