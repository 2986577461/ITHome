package com.xiaoyan.context;

public class BaseContext {

    public static final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public static void setCurrentStudentId(String id) {
        threadLocal.set(id);
    }

    public static String getCurrentStudentId(){
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
