package com.xiaoyan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.PasswordDTO;

import com.xiaoyan.pojo.Student;
import com.xiaoyan.vo.StudentVO;
import java.util.List;


public interface UsersService extends IService<Student> {

    StudentVO getUser(Integer studentId);

    StudentVO login(LoginDTO message);

    void removeStudents(List<Long> studentIds);

    List<StudentVO> getAll();

    void update(Student student);

    void updatePassword(PasswordDTO passwordDTO,Integer studentId);

}