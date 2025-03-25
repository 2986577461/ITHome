package com.xiaoyan.exception;


//参数异常时抛出
public class ParameterException extends RuntimeException{

    public ParameterException(String message) {
        super(message);
    }
}
