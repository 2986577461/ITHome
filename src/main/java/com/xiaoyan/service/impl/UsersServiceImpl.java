package com.xiaoyan.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.dto.StudentAccount;
import com.xiaoyan.dto.UpdateStudent;
import com.xiaoyan.exception.LoginConditionException;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.ITStudent;

import com.xiaoyan.service.UsersService;
import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

import java.util.ArrayList;


@Service
public class UsersServiceImpl extends ServiceImpl<UserMapper, ITStudent>
        implements UsersService {

    @Resource
    private UserMapper userMapper;

    @Override
    public ITStudent getUser(String id) {
        ITStudent s = userMapper.selectById(id);
        if (s == null) {
            //
            return null;
        }
        s.setPassword("");
        return s;

    }

    @Override
    public boolean login(StudentAccount message) {
        ITStudent student = userMapper.selectById(message.getId());
        if (student == null) return false;

        String oldPswd = student.getPassword();
        if (oldPswd != null && oldPswd.equals(message.getPswd())) {
            //将该id设置为登陆，令牌过期时间在配置文件中设为了7天
            StpUtil.login(message.getId());
            return true;
        }
        return false;
    }

    @Override
    public boolean removeStudents(ArrayList<String> students) {
        for (String id : students)
            if (userMapper.selectById(id) == null)
                return false;

        for (String id : students)
            userMapper.deleteById(id);

        return true;
    }


    @Override
    public ArrayList<ITStudent> selectAllMember() {
        return (ArrayList<ITStudent>) userMapper.selectList(null);
    }


    @Override
    public boolean updateStudent(String id, UpdateStudent student) {

        LambdaUpdateWrapper<ITStudent>luw=new LambdaUpdateWrapper<>();

        luw.set(ITStudent::getName,student.getName())
                .set(ITStudent::getSex,student.getSex())
                .set(ITStudent::getMajor,student.getMajor())
                .set(ITStudent::getPassword,student.getPassword())
                .set(ITStudent::getClaxx,student.getClaxx())
                .set(ITStudent::getAcademy,student.getAcademy())
                .set(ITStudent::getPosition,student.getPosition())
                .set(ITStudent::getStudentId,student.getStudentId())
                .eq(ITStudent::getStudentId,id);

        return userMapper.update(luw)==1;
    }

    @Override
    public boolean updatePassword(String id, String pswd) {
        if (!StpUtil.isLogin())
            throw new LoginConditionException("更改密码时未登陆");

        LambdaUpdateWrapper<ITStudent> luw = new LambdaUpdateWrapper<>();
        luw.set(ITStudent::getPassword, pswd).eq(ITStudent::getStudentId, id);
        return userMapper.update(luw) == 1;
    }

}
