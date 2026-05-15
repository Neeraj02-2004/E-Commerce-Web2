package com.neeraj.SpringEcom.exception;

public class OrderAlreadyCancelledException extends RuntimeException {

    public OrderAlreadyCancelledException(String orderId) {
        super("Order is already cancelled: " + orderId);
    }
}
