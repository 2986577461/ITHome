package com.xiaoyan.exception;

public class PasswordErrorException extends RuntimeException {
    public PasswordErrorException(String message) {
        super(message);
    }
}
