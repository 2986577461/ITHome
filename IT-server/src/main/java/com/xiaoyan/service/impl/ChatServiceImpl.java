package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.service.ChatService;
import com.xiaoyan.vo.StudentDialogVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ChatServiceImpl implements ChatService {

    private UserMapper userMapper;

    @Override
    public List<StudentDialogVO> getList() {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(Student::getId, BaseContext.getCurrentId());
        List<Student> list = userMapper.selectList(wrapper);

        List<StudentDialogVO> studentDialogVOList = new ArrayList<>();

        for (Student student1 : list) {
            StudentDialogVO studentDialogVO = new StudentDialogVO();
            studentDialogVO.setId(student1.getId());
            studentDialogVO.setName(student1.getName());
            studentDialogVOList.add(studentDialogVO);
        }
        return studentDialogVOList;
    }
}


