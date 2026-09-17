package com.tamar.computerstore.exception;

public class StaleProductVersionException extends RuntimeException {

    public StaleProductVersionException(Long id, long expectedVersion, long currentVersion) {
        super("Product " + id + " was modified by another request (expected version "
                + expectedVersion + ", current version " + currentVersion + ")");
    }
}
