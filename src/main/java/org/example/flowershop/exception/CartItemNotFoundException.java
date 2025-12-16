package org.example.flowershop.exception;

public class CartItemNotFoundException extends NotFoundException {
    public CartItemNotFoundException(String message) {
        super(message);
    }
}
