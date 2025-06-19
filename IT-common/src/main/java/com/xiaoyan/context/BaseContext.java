package com.xiaoyan.context;

import com.xiaoyan.vo.StudentVO;

public class BaseContext {

    public static final ThreadLocal<StudentVO> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(StudentVO studentVO) {
        threadLocal.set(studentVO);
    }

    public static Long getCurrentId() {
        StudentVO studentVO = threadLocal.get();
        return studentVO.getId();
    }
    public static StudentVO getCurrentStudent(){
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
