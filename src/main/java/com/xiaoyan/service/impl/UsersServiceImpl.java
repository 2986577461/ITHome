package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.UpdateStudent;
import com.xiaoyan.exception.AccountNotFoundException;
import com.xiaoyan.exception.LoginConditionException;
import com.xiaoyan.exception.PasswordErrorException;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.ITStudent;

import com.xiaoyan.service.UsersService;
import com.xiaoyan.vo.ITStudentVO;
import com.xiaoyan.vo.StudentGovernVO;
import jakarta.annotation.Resource;

import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class UsersServiceImpl extends ServiceImpl<UserMapper, ITStudent>
        implements UsersService {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    @Resource
    private UserMapper userMapper;

    @Override
    public ITStudentVO getUser() {
        String currentId = BaseContext.getCurrentId();
        ITStudent student = userMapper.selectById(currentId);
        if (student == null)
            throw new LoginConditionException(MessageConstant.USER_NOT_LOGIN);

        ITStudentVO studentVO = new ITStudentVO();
        BeanUtils.copyProperties(student, studentVO);

        return studentVO;

    }

    @Override
    public ITStudentVO login(LoginDTO message) {
        String username = message.getUsername();
        String password = message.getPassword();

        ITStudent student = userMapper.selectById(username);
        if (student == null)
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);

        if (!encoder.matches(password, student.getPassword()))
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);

        ITStudentVO studentVO = new ITStudentVO();
        BeanUtils.copyProperties(student, studentVO);

        BaseContext.setCurrentId(username);

        return studentVO;
    }

    @Override
    public void removeStudents(ArrayList<String> students) {
        for (String id : students)
            userMapper.selectById(id);


        for (String id : students)
            userMapper.deleteById(id);

    }


    @Override
    public List<ITStudent> selectAllMember() {
        return userMapper.selectList(null);
    }


    @Override
    public void updateStudent(String id, UpdateStudent student) {

        LambdaUpdateWrapper<ITStudent> luw = new LambdaUpdateWrapper<>();

        luw.set(ITStudent::getName, student.getName())
                .set(ITStudent::getSex, student.getSex())
                .set(ITStudent::getMajor, student.getMajor())
                .set(ITStudent::getPassword, student.getPassword())
                .set(ITStudent::getClaxx, student.getClaxx())
                .set(ITStudent::getAcademy, student.getAcademy())
                .set(ITStudent::getPosition, student.getPosition())
                .set(ITStudent::getStudentId, student.getStudentId())
                .eq(ITStudent::getStudentId, id);

        userMapper.update(luw);
    }

    @Override
    public void updatePassword(String id, String pswd) {
        if (BaseContext.getCurrentId() == null)
            throw new LoginConditionException("更改密码时未登陆");

        LambdaUpdateWrapper<ITStudent> luw = new LambdaUpdateWrapper<>();
        luw.set(ITStudent::getPassword, pswd).eq(ITStudent::getStudentId, id);
        userMapper.update(luw);
    }

    @Override
    public List<StudentGovernVO> getAll() {
        List<ITStudent> list = userMapper.selectList(null);

        List<StudentGovernVO> governVOS = new ArrayList<>();

        for (ITStudent student : list) {
            StudentGovernVO vo = new StudentGovernVO();
            BeanUtils.copyProperties(student, vo);
            governVOS.add(vo);
        }

        return governVOS;
    }


}
