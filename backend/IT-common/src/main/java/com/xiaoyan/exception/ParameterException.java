package com.xiaoyan.exception;


import com.xiaoyan.result.Result;
import lombok.Getter;

/**
 * 业务异常：可以预期、需要把原因告诉调用方的失败。
 *
 * <p>默认按 400 返回。权限不足、未登录这类语义不同的场景，
 * 在抛出时显式指定 code，交给全局异常处理器原样透出。</p>
 */
@Getter
public class ParameterException extends RuntimeException {

    private final int code;

    public ParameterException(String message) {
        this(Result.BAD_REQUEST, message);
    }

    public ParameterException(int code, String message) {
        super(message);
        this.code = code;
    }

}