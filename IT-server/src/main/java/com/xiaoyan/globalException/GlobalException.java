package com.xiaoyan.globalException;


import com.xiaoyan.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalException {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();

        // 核心改动在这里：
        // 我们遍历所有字段错误，为每个错误生成 "字段名 错误信息" 的格式
        String errorMsg = bindingResult.getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        // 返回统一格式的错误响应
        return Result.error(errorMsg);
    }
    /**
     * 处理 @RequestParam, @PathVariable, @RequestHeader 等参数校验失败的异常
     * 当这些参数使用 @NotBlank, @NotNull, @Min 等注解校验失败时，会抛出 ConstraintViolationException
     * (注意：需要确保在 Controller 上或应用入口类上添加 @Validated 注解才能触发此类校验)
     *
     * @param e ConstraintViolationException 异常对象
     * @return 统一的Result响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> handleConstraintViolationException(ConstraintViolationException e) {
        String defaultMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        // 返回自定义的错误码和消息
        return Result.error("参数校验失败: " + defaultMessage);
    }

    @ExceptionHandler
    public Result<String> exceptionHandler(RuntimeException ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }
}
