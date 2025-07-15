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

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        if (student == null)
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);

        if (!encoder.matches(password, student.getPassword()))
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);

        StudentVO studentVO = new StudentVO();
        BeanUtils.copyProperties(student, studentVO);

        BaseContext.setCurrentStudentId(studentVO.getStudentId());

        return studentVO;
    }

    @Override
    @CacheEvict(cacheNames = {"userList"},allEntries = true)
    public void removeStudents(List<Long> ids) {
        userMapper.deleteByIds(ids);
    }


    @Override
    @Cacheable(value = "userList",key = "'userList'")
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
    @CacheEvict(cacheNames = {"userList","articlesList"},allEntries = true)
    public void update(Student student) {
        String password = student.getPassword();
        if(password!=null)
            student.setPassword(encoder.encode(password));
        userMapper.updateById(student);
    }

    @Override
    public void updatePassword(PasswordDTO passwordDTO, Integer studentId) {
        userMapper.updatePasswordByStudentId(
                studentId, encoder.encode(passwordDTO.getPassword()));
    }



}
