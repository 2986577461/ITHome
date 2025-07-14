package com.xiaoyan.service;

import com.xiaoyan.dto.MessageDTO;
import com.xiaoyan.vo.AiDialogSessionVO;
import com.xiaoyan.vo.AiDialogVO;
import reactor.core.publisher.Flux;

import java.util.List;


public interface AiService {
    Flux<String> streamChatCompletion(MessageDTO messageDTO,Integer studentId);

    List<AiDialogSessionVO> getAll(Integer studentId);

    List<AiDialogVO> getMessages(Integer sessionId);

    void deleteSession(Integer sessionId,Integer studentId);
}
