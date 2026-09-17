package com.tamar.computerstore.exception;

public class ProductInUseException extends RuntimeException {

    public ProductInUseException(Long id) {
        super("Product " + id + " cannot be deleted because it is referenced by one or more orders");
    }
}
