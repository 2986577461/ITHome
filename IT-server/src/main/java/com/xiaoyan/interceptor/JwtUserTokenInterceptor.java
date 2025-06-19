package com.xiaoyan.interceptor;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.vo.StudentVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtUserTokenInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getTokenName());
        //2、校验令牌
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
            Object studentVoAsObject = claims.get(jwtProperties.getTokenName());

            StudentVO studentVO = objectMapper.convertValue(studentVoAsObject, StudentVO.class);

            BaseContext.setCurrentId(studentVO);
            return true;
        } catch (Exception ex) {
            log.info("请求被拒绝");
            //4、不通过，响应401状态码
            response.setStatus(401);
            return false;
        }
    }
}
