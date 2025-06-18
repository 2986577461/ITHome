package com.xiaoyan.service.impl;

import com.xiaoyan.dto.EmailDTO;
import com.xiaoyan.service.EmailService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    private JavaMailSender mailSender;

    public final static Map<String , CodeInfo> codePool = new ConcurrentHashMap<>();

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Data
    @ToString
    public static class CodeInfo {
        private String code;
        private long expireTime;

        public CodeInfo(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }

    /**
     * 发送验证码邮件
     *
     * @param email 目标邮箱
     */
    public void sendVerificationCode(String email) {
        // 1. 生成6位随机验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        codePool.put(email, new CodeInfo(code, System.currentTimeMillis()+5*60*1000));

        // 3. 发送邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("马家骑服务平台");
        message.setText("验证码: " + code + "，5分钟内有效");
        mailSender.send(message);
    }

    public boolean verifyCode(EmailDTO emailDTO) {
        String email = emailDTO.getEmail();
        String inputCode = emailDTO.getCode();

        CodeInfo codeInfo = codePool.get(email);
        if (codeInfo == null) return false;
        // 检查未过期且验证码匹配

        return System.currentTimeMillis() <= codeInfo.getExpireTime()
                && inputCode.equals(codeInfo.getCode());
    }
}