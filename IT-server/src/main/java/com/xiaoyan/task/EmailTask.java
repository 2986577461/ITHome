package com.xiaoyan.task;


import com.xiaoyan.service.impl.EmailServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class EmailTask {

   private static final Map<String, EmailServiceImpl.CodeInfo> codePool = EmailServiceImpl.codePool;

    @Scheduled(cron = "0 * * * * *")
    public void emailTask() {
        List<String> list = new ArrayList<>();
        codePool.forEach((s, codeInfo) -> {
            if (System.currentTimeMillis() > codeInfo.getExpireTime())
                list.add(s);
        });
        log.info("定期清理验证码:{}",list);
        for (String s : list)
            codePool.remove(s);


    }
}
