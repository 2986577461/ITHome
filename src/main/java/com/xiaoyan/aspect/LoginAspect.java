package com.xiaoyan.aspect;


import cn.dev33.satoken.stp.StpUtil;
import com.xiaoyan.exception.LoginConditionException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoginAspect {

    @Before("@annotation(com.xiaoyan.annotation.RequireLogin)")
    public void validateIsStudent(){
        if (!StpUtil.isLogin())
            throw new LoginConditionException("未登陆");
    }



}
