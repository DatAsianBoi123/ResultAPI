package com.datasiqn.resultapi;

public class ExpectException extends RuntimeException {
    public ExpectException(String message, Object error) {
        super(message + ": " + error);
    }
}
