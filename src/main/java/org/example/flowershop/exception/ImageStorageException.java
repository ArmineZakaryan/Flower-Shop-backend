package org.example.flowershop.exception;

import java.io.IOException;

public class ImageStorageException extends RuntimeException {
    public ImageStorageException(String failedToSaveImage, IOException e) {
    }
}

