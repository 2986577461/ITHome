package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.constant.PositionConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.PasswordDTO;
import com.xiaoyan.exception.AccountNotFoundException;
import com.xiaoyan.exception.PasswordErrorException;
import com.xiaoyan.interceptor.JwtWhiteList;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;

import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.vo.StudentVO;

import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuchao
 */
@Service
@AllArgsConstructor
public class UsersServiceImpl extends ServiceImpl<UserMapper, Student>
        implements UsersService {

    private JwtProperties jwtProperties;

    private JwtWhiteList jwtWhiteList;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private UserMapper userMapper;

    @Override
    public StudentVO getUser(Integer studentId) {
        Student student = userMapper.selectByStudentId(studentId);
        StudentVO studentVO = new StudentVO();
        BeanUtils.copyProperties(student, studentVO);

        return studentVO;
    }

    @Override
    public StudentVO login(LoginDTO message) {
        Integer studentId = message.getStudentId();
        String password = message.getPassword();
        Student student = userMapper.selectByStudentId(studentId);
        if (student == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if (!ENCODER.matches(password, student.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        StudentVO studentVO = new StudentVO();
        BeanUtils.copyProperties(student, studentVO);

        BaseContext.setCurrentStudentId(studentVO.getStudentId());

        String position = studentVO.getPosition();
        String tokenName;
        if(position.equals(PositionConstant.STUDENT)) {
            tokenName= JwtClaimsConstant.USER_ID;
        } else {
            tokenName=JwtClaimsConstant.ADMIN_ID;
        }

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(tokenName, studentVO.getStudentId());
        String token = JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                claims);
        studentVO.setToken(token);

        //添加到token白名单
        jwtWhiteList.addOrUpdateTokenHash(token);

        return studentVO;
    }

    @Override
    public void removeStudents(List<Long> ids) {
        userMapper.deleteByIds(ids);
    }


    @Override
    public List<StudentVO> getAll() {
        List<Student> students = userMapper.selectList(null);
        List<StudentVO> studentVos = new ArrayList<>();
        for (Student student : students) {
            StudentVO studentVO = new StudentVO();
            BeanUtils.copyProperties(student, studentVO);
            studentVos.add(studentVO);
        }
        return studentVos;
    }


    @Override
    public void update(Student student) {
        String password = student.getPassword();
        if(password!=null) {
            student.setPassword(ENCODER.encode(password));
        }
        userMapper.updateById(student);
    }

    @Override
    public void updatePassword(PasswordDTO passwordDTO, Integer studentId) {
        userMapper.updatePasswordByStudentId(
                studentId, ENCODER.encode(passwordDTO.getPassword()));
    }



}