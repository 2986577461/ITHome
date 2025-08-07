package com.xiaoyan.service.impl;

import com.xiaoyan.mapper.ChatMapper;
import com.xiaoyan.pojo.ChatDialog;
import com.xiaoyan.service.ChatService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.vo.StudentDialogVO;
import com.xiaoyan.vo.StudentVO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
public class ChatServiceImpl implements ChatService {

    private UsersService usersService;

    private ChatMapper chatMapper;


    @Override
    public List<StudentDialogVO> getAllUser() {
        List<StudentVO> all = usersService.getAll();
        List<StudentDialogVO> list = new ArrayList<>();
        for (StudentVO studentVO : all) {
            StudentDialogVO studentDialogVO = new StudentDialogVO();
            BeanUtils.copyProperties(studentVO, studentDialogVO);
            list.add(studentDialogVO);
        }
        return list;
    }

    @Override
    public void saveMessage(ChatDialog chatDialog) {
        chatMapper.insert(chatDialog);
    }

    @Override
    public List<ChatDialog> getAllHistoryMessage(Integer studentId) {
        return chatMapper.selectHistoryMessageByStudentId(studentId);
    }
}


