package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.PasswordDTO;
import com.xiaoyan.exception.AccountNotFoundException;
import com.xiaoyan.exception.PasswordErrorException;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;

import com.xiaoyan.service.UsersService;
import com.xiaoyan.vo.StudentVO;
import jakarta.annotation.Resource;

import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Service
@Validated
public class UsersServiceImpl extends ServiceImpl<UserMapper, Student>
        implements UsersService {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    @Resource
    private UserMapper userMapper;

    @Override
    public StudentVO getUser() {
        return BaseContext.getCurrentStudent();
    }

    @Override
    public StudentVO login(LoginDTO message) {
        Long id = message.getId();
        String password = message.getPassword();

        Student student = userMapper.selectById(id);
        if (student == null)
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);

        if (!encoder.matches(password, student.getPassword()))
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);

        StudentVO studentVO = new StudentVO();
        BeanUtils.copyProperties(student, studentVO);

        BaseContext.setCurrentId(studentVO);

        return studentVO;
    }

    @Override
    public void removeStudents(List<Long> ids) {
        this.removeBatchByIds(ids);
    }


    @Override
    public List<StudentVO> getAll() {
        List<Student> students = userMapper.selectList(null);
        List<StudentVO> studentVOS = new ArrayList<>();

        for (Student student : students) {
            StudentVO studentVO = new StudentVO();
            BeanUtils.copyProperties(student, studentVO);
            studentVOS.add(studentVO);
        }
        return studentVOS;
    }


    @Override
    public void update( Student student) {
        userMapper.updateById(student);
    }

    @Override
    public void updatePassword(PasswordDTO passwordDTO) {
        Long id = BaseContext.getCurrentId();
        userMapper.updatePasswordById(id,encoder.encode(passwordDTO.getPassword()));
    }

}
