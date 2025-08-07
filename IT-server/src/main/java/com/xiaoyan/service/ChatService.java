package com.xiaoyan.service;


import com.xiaoyan.pojo.ChatDialog;
import com.xiaoyan.vo.StudentDialogVO;

import java.util.List;

public interface ChatService {
    List<StudentDialogVO> getAllUser();

    void saveMessage(ChatDialog chatDialog);

    List<ChatDialog> getAllHistoryMessage(Integer studentId);
}
