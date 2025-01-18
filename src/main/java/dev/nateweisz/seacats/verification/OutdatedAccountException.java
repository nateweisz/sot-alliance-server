package dev.nateweisz.seacats.verification;

public class OutdatedAccountException extends RuntimeException {
    public OutdatedAccountException(String message) {
        super(message);
    }
}
