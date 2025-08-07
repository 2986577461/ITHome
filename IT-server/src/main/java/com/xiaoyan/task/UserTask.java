package com.xiaoyan.task;

import com.xiaoyan.interceptor.JwtWhiteList;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class UserTask {

    private JwtWhiteList jwtWhiteList;

//    @Scheduled(cron = "0/5 * * * * *")
    public void deleteAllUser(){
//        System.out.println(jwtWhiteList.validation(20230057, ));
    }
}
