package com.xiaoyan.aspect;


import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.StudentDTO;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
//@Component
public class UpdateStudentAspect {

    @Resource
    private UserMapper userMapper;

    @Before("@annotation(com.xiaoyan.annotation.CheckPrimaryKeyRepeat)")
    public void updateStudent(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String id = (String) args[0];
        StudentDTO student = (StudentDTO) args[1];

        //如果改了学号
        if (!id.equals(student.getId())) {

            //如果学号已存在
            if (userMapper.selectById(student.getId()) != null)
                throw new ParameterException("学号重复");
            else
                BaseContext.removeCurrentId();
        }
    }
}
