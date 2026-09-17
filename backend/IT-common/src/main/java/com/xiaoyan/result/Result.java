package com.xiaoyan.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回结果。
 *
 * <p>注意：HTTP 状态码恒为 200，业务结果全部由 body 里的 {@code code} 表达。
 * 前端的 axios 响应拦截器只在 2xx 时返回 body，非 2xx 会走错误分支且不返回数据，
 * 所以这里不能改 HTTP 状态码，否则调用方拿不到 code/msg。</p>
 *
 * @param <T>
 */
@Data
public class Result<T> implements Serializable {

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int PAYLOAD_TOO_LARGE = 413;
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int SERVER_ERROR = 500;
    public static final int SERVICE_UNAVAILABLE = 503;

    private Integer code; //编码：200成功，其它为失败，取值见上面的常量
    private String msg; //错误信息
    private T data; //数据

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<>();
        result.data = object;
        result.code = SUCCESS;
        return result;
    }

    /** 默认按「无权限」返回，等价于 {@code error(FORBIDDEN, msg)} */
    public static <T> Result<T> error(String msg) {
        return error(FORBIDDEN, msg);
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.msg = msg;
        result.code = code;
        return result;
    }

}
