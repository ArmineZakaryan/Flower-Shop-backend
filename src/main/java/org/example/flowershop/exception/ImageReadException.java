package org.example.flowershop.exception;

import java.io.IOException;

public class ImageReadException extends RuntimeException {
    public ImageReadException(String message, IOException e) {
        super(message);
    }
}
