package com.xiaoyan.aspect;


import cn.dev33.satoken.stp.StpUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Aspect
@Component
public class LogoutAspect {

    @After("@annotation(com.xiaoyan.annotation.RequireLogout)")
    public void logout(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        ArrayList<String> list = (ArrayList<String>) args[0];
        list.forEach(StpUtil::logout);


    }
}
