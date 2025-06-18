package com.xiaoyan.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserTask {

//    @Scheduled()
    public void deleteAllUser(){
        log.info("协会换届，触发删除所有学生");
        //todo
        //协会换届删除操作
    }
}
