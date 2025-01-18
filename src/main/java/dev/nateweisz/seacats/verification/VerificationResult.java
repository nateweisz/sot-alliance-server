package dev.nateweisz.seacats.verification;

import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Optional;

public sealed interface VerificationResult {
    // meaning they are good to go
    record NothingFound(String linkedAccount) implements VerificationResult {}
    record UserBlacklisted() implements VerificationResult {}
    record BlacklistedFriends(List<Pair<String, String>> blacklistedFriends) implements VerificationResult {}
    record NoAccountLinked() implements VerificationResult {}
    record OutdatedAccountLinked() implements VerificationResult {}
    record Failed(Exception cause) implements VerificationResult {}
}