package com.tamar.computerstore.exception;

import com.tamar.computerstore.entity.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException {

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Invalid order status transition: " + from + " -> " + to);
    }
}
