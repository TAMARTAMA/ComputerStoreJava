package com.tamar.computerstore.exception;

public class InventoryConflictException extends RuntimeException {

    public InventoryConflictException() {
        super("Inventory was modified by another request; reload and retry");
    }
}