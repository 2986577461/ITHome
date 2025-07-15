package com.xiaoyan.service;

import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.PasswordDTO;

import com.xiaoyan.pojo.Student;
import com.xiaoyan.vo.StudentVO;
import java.util.List;


public interface UsersService {

    StudentVO getUser(Integer studentId);

    StudentVO login(LoginDTO message);

    void removeStudents(List<Long> ids);

    List<StudentVO> getAll();

    void update(Student student);

    void updatePassword(PasswordDTO passwordDTO,Integer studentId);

}
