package com.xiaoyan.service;

import com.xiaoyan.vo.AiDialogSessionVO;
import com.xiaoyan.vo.AiDialogVO;

import java.util.List;

public interface AiSessionService {
    Long createSession(Integer studentId);

    void deleteSession(Long sessionId,Integer studentId);

    List<AiDialogVO> getMessages(Long sessionId);

    List<AiDialogSessionVO> getAll(Integer studentId);

}
