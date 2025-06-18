package com.xiaoyan.service;

import com.xiaoyan.dto.EmailDTO;

public interface EmailService {

    void sendVerificationCode(String email);

    boolean verifyCode(EmailDTO emailDTO);
}
