package com.xiaoyan.service;

import com.xiaoyan.vo.AiDialogSessionVO;
import com.xiaoyan.vo.AiDialogVO;

import java.util.List;

public interface AiSessionService {
    Integer createSession(Integer studentId);

    void deleteSession(Integer sessionId,Integer studentId);

    List<AiDialogVO> getMessages(Integer sessionId);

    List<AiDialogSessionVO> getAll(Integer studentId);

}
