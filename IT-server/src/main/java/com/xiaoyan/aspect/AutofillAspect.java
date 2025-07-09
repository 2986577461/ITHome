package com.xiaoyan.aspect;


import com.xiaoyan.annotation.AutoFillFields;
import com.xiaoyan.conmon.BaseEntity;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.vo.StudentVO;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class AutofillAspect {

//    @Pointcut("@annotation(com.xiaoyan.annotation.AutoFillFields)")
    public void autoFillPointcut() {
    }

//    @Before("autoFillPointcut() && @annotation(autoFillFields)")
    public void parameterVerifiction(JoinPoint joinPoint, AutoFillFields autoFillFields) {
//        BaseEntity base = (BaseEntity) joinPoint.getArgs()[0];
//        StudentVO studentVO = BaseContext.getCurrentStudentId();
//        LocalDateTime now = LocalDateTime.now();
//
//        if (autoFillFields.value() == AutoFillFields.OpType.INSERT) {
//            base.setCreatedDateTime(now);
//        }
//        base.setUpdatedDateTime(now);
//        base.setStudentId(studentVO.getId());
//        base.setStudentName(studentVO.getName());

    }
}
