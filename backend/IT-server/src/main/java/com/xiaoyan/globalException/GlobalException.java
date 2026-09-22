package com.xiaoyan.globalException;


import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * <p>分三类，处理方式和日志级别都不同：</p>
 * <ul>
 *   <li><b>业务异常</b>——预期内的失败，WARN 记一行不打堆栈，原因原样返回给调用方</li>
 *   <li><b>请求本身有问题</b>——参数缺失/类型不对/JSON 格式错/方法不支持/文件过大，WARN，返回对应 4xx 语义的 code</li>
 *   <li><b>其它一切</b>——ERROR 打完整堆栈，对外只回一句固定文案</li>
 * </ul>
 *
 * <p>所有响应都保持 HTTP 200，业务结果由 body 里的 code 表达，原因见 {@link Result}。</p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalException {

    /* ==================== 业务异常 ==================== */

    @ExceptionHandler(ParameterException.class)
    public Result<Void> handleParameterException(ParameterException e, HttpServletRequest request) {
        log.warn("业务异常 {} {} -> {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /* ==================== 请求参数问题 ==================== */

    /** @Valid 校验 @RequestBody 失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleBodyValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", "));
        return badRequest(request, message);
    }

    /** Controller 上标了 @Validated 时，方法参数上的约束（@Min / @NonNull 等）失败走这里 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public Result<Void> handleMethodValidation(HandlerMethodValidationException e, HttpServletRequest request) {
        String message = e.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", "));
        return badRequest(request, message);
    }

    /** 请求体不是合法 JSON，或该带 body 的请求没带 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体解析失败 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(Result.BAD_REQUEST, MessageConstant.REQUEST_FORMAT_ERROR);
    }

    /** 路径变量 / 查询参数类型不对，比如 ?page=abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return badRequest(request, "参数 " + e.getName() + " 格式不正确");
    }

    /** 缺少必填的查询参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParameter(MissingServletRequestParameterException e,
                                               HttpServletRequest request) {
        return badRequest(request, "缺少必填参数 " + e.getParameterName());
    }

    /* ==================== 请求本身不合法 ==================== */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                 HttpServletRequest request) {
        log.warn("请求方法不支持 {} {}", request.getMethod(), request.getRequestURI());
        return Result.error(Result.METHOD_NOT_ALLOWED, MessageConstant.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e,
                                                    HttpServletRequest request) {
        log.warn("Content-Type 不支持 {} {}: {}", request.getMethod(), request.getRequestURI(),
                e.getContentType());
        return Result.error(Result.UNSUPPORTED_MEDIA_TYPE, MessageConstant.CONTENT_TYPE_NOT_SUPPORTED);
    }

    /** 上传超过 spring.servlet.multipart 限制（当前配的是单文件 500MB） */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("上传超过大小限制 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(Result.PAYLOAD_TOO_LARGE, MessageConstant.FILE_TOO_LARGE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("接口不存在 {} {}", request.getMethod(), request.getRequestURI());
        return Result.error(Result.NOT_FOUND, MessageConstant.RESOURCE_NOT_FOUND);
    }

    /* ==================== 数据层 ==================== */

    /** 唯一索引冲突。业务上并发重复提交由它兜底（比如 newcomer.student_id 的 unique） */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("唯一键冲突 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.error(Result.BAD_REQUEST, MessageConstant.DATA_DUPLICATE);
    }

    /* ==================== 兜底 ==================== */

    /**
     * 没被上面任何一条接住的异常。
     *
     * <p>对外只回一句固定文案：异常信息里可能带着 SQL、类名、文件路径，
     * 原样返回等于把内部实现暴露给调用方。真正的原因写进日志。</p>
     *
     * <p>日志里只记 URI、不记 query string——JWT 支持从查询参数取
     * （见 {@code JwtUserTokenInterceptor}），打全量 URL 会把 token 写进日志。</p>
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("未处理异常 {} {}", request.getMethod(), request.getRequestURI(), e);
        return Result.error(Result.SERVER_ERROR, MessageConstant.SERVER_ERROR);
    }

    private Result<Void> badRequest(HttpServletRequest request, String message) {
        String reason = (message == null || message.isBlank())
                ? MessageConstant.PARAMETER_VALIDATE_FAILED : message;
        log.warn("参数校验失败 {} {} -> {}", request.getMethod(), request.getRequestURI(), reason);
        return Result.error(Result.BAD_REQUEST, reason);
    }

    /**
     * 数据访问异常：SQL 语法错误、死锁、连接池耗尽等 MyBatis / MySQL 的问题。
     *
     * <p>注意这里接不到 Redis 的异常——{@code RedisUtil} 已经在内部按用途分别消化掉了
     * （缓存读降级查库、缓存失效记日志、登录态放行），不会有 Redis 异常冒到 Controller。
     * 所以日志文案不能写成「Redis 连接失败」，否则 MySQL 出问题时会被误导着去查 Redis。</p>
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccess(DataAccessException e, HttpServletRequest request) {
        log.error("数据访问异常 {} {}", request.getMethod(), request.getRequestURI(), e);
        return Result.error(Result.SERVER_ERROR, MessageConstant.SERVER_ERROR);
    }
}
