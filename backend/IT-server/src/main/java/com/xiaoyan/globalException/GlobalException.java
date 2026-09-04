package com.xiaoyan.globalException;


import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalException {

//     处理自定义业务异常
    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result<Void> handleBusinessException(HandlerMethodValidationException e) {
        return Result.error(e.getMessage());
    }

    // 参数校验失败（@Valid 触发）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(message);
    }

    @ExceptionHandler(ParameterException.class)
    public Result<Void> handleException(ParameterException e) {
        return Result.error(e.getMessage());
    }
}