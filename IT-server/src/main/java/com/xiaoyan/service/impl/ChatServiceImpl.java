package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.service.ChatService;
import com.xiaoyan.vo.StudentDialogVO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class ChatServiceImpl implements ChatService {

    private UserMapper userMapper;

    @Override
    public List<StudentDialogVO> getList() {
        List<Student> students = userMapper.selectList(null);
        System.out.println(students);
        for (Student student : students) {
            if (student.getId().equals(BaseContext.getCurrentId())) ;
            students.remove(student);
        }
        System.out.println(students);
        List<StudentDialogVO> studentDialogVOList = new ArrayList<>();


        for (Student student1 : students) {
            StudentDialogVO studentDialogVO = new StudentDialogVO();
            BeanUtils.copyProperties(student1, studentDialogVO);
            studentDialogVOList.add(studentDialogVO);
        }
        return studentDialogVOList;
    }
}


