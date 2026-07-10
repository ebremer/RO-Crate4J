package com.ebremer.rocrate4j;

/**
 * Unchecked exception for RO-Crate processing failures (e.g. JSON-LD
 * serialization errors) that callers may catch as a single library type.
 *
 * @author erich
 */
public class ROCrateException extends RuntimeException {

    public ROCrateException(String message) {
        super(message);
    }

    public ROCrateException(String message, Throwable cause) {
        super(message, cause);
    }
}
