package com.xiaoyan.service;

import com.xiaoyan.dto.MessageDTO;
import com.xiaoyan.vo.AiDialogGroupVO;
import com.xiaoyan.vo.AiDialogVO;
import reactor.core.publisher.Flux;

import java.util.List;


public interface AiService {
    Flux<String> streamChatCompletion(Integer studentId,Integer groupId, String message);

    void saveAnswer(MessageDTO messageDTO);

    List<AiDialogGroupVO> getAll();

    List<AiDialogVO> getMessages(Integer sessionId);

    void deleteSession(Integer sessionId);
}
