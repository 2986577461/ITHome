package com.xiaoyan.aspect;


import cn.dev33.satoken.stp.StpUtil;
import com.xiaoyan.exception.LoginConditionException;
import com.xiaoyan.exception.PositionException;

import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.ITStudent;
import jakarta.annotation.Resource;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AdminAspect {

    @Resource
    private UserMapper userMapper;

    @Before("@annotation(com.xiaoyan.annotation.RequireAdmin)")
    public void validateUser() {
        if (!StpUtil.isLogin())
            throw new LoginConditionException("未登陆");

        ITStudent student = userMapper.selectById((String) StpUtil.getLoginId());

        if(student==null||!(student.getPosition().equals("会长")||student.getPosition().equals("副会长")))
            throw new PositionException("令牌或管理员身份异常");

    }


}
