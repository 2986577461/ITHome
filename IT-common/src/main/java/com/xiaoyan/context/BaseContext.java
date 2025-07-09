package com.xiaoyan.context;

public class BaseContext {

    public static final ThreadLocal<Integer> threadLocal = new ThreadLocal<>();

    public static void setCurrentStudentId(Integer id) {
        threadLocal.set(id);
    }

    public static Integer getCurrentStudentId(){
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
