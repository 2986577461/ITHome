package com.xiaoyan.interceptor;


import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
@AllArgsConstructor
public class JwtUserTokenInterceptor implements HandlerInterceptor {

    private JwtProperties jwtProperties;

    private JwtWhiteList jwtWhiteList;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getTokenName());

        if(token==null){
            token=request.getParameter(jwtProperties.getTokenName());
        }

        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
            Object userId = claims.get(JwtClaimsConstant.USER_ID);
            Object adminId = claims.get(JwtClaimsConstant.ADMIN_ID);
            Object rawStudentId = userId != null ? userId : adminId;
            if (rawStudentId == null) {
                response.setStatus(401);
                return false;
            }
            String studentId = rawStudentId.toString();

            if (!jwtWhiteList.validation(studentId, token)) {
                response.setStatus(401);
                return false;
            }

            BaseContext.setCurrentStudentId(studentId);
            return true;

        } catch (RuntimeException ex) {
            response.setStatus(401);
            return false;
        }
    }
}