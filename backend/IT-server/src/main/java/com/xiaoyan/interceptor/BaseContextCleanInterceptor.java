package com.xiaoyan.interceptor;

import com.xiaoyan.context.BaseContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求结束后清理 {@link BaseContext} 的 ThreadLocal。
 *
 * <p>Tomcat 复用工作线程，不清理的话上一个请求的 studentId 会残留在线程上，
 * 被下一个请求（尤其是免登录白名单里的接口）读到，造成越权或数据错乱。</p>
 */
@Component
public class BaseContextCleanInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        BaseContext.removeCurrentId();
    }
}
