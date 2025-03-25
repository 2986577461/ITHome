package com.xiaoyan.exception;



//身份异常时抛出
public class PositionException extends RuntimeException{

    public PositionException(String message) {
        super(message);
    }
}
