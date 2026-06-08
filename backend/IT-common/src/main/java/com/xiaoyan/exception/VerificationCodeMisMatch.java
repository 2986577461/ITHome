package com.xiaoyan.exception;

public class VerificationCodeMisMatch extends RuntimeException {
    public VerificationCodeMisMatch(String message) {
        super(message);
    }
}
