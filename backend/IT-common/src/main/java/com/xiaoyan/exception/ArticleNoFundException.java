package com.xiaoyan.exception;

public class ArticleNoFundException extends RuntimeException {
    public ArticleNoFundException(String message) {
        super(message);
    }
}
