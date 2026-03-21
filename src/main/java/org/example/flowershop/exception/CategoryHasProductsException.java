package org.example.flowershop.exception;

public class CategoryHasProductsException extends RuntimeException {
    public CategoryHasProductsException(String message) {
        super(message);
    }
}
