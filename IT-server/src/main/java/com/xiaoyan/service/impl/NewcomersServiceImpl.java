package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.constant.PasswordConstant;
import com.xiaoyan.constant.PositionConstant;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.exception.RepeatRuestException;
import com.xiaoyan.mapper.NewcomerMapper;

import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.service.NewcomersService;
import com.xiaoyan.vo.NewcomerVO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Validated
public class NewcomersServiceImpl extends ServiceImpl<NewcomerMapper, Newcomer>
        implements NewcomersService {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private NewcomerMapper newcomerMapper;

    private UserMapper userMapper;

    @Override
    public void refuseNewcomer(Integer id) {
      newcomerMapper.deleteById(id);
    }

    @Override
    public void agreeNewcomer(Integer id) {
        Newcomer newcomer = newcomerMapper.selectById(id);
        if (newcomer == null)
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);

        Student student = userMapper.selectById(newcomer.getStudentId());

        if (student != null)
            throw new RuntimeException(MessageConstant.REPEATREQUEST);

        newcomerMapper.deleteById(id);

        student = new Student();
        BeanUtils.copyProperties(newcomer, student, "id");
        student.setPassword(encoder.encode(PasswordConstant.STUDENT_PASSWORD));
        student.setPosition(PositionConstant.STUDENT);

        userMapper.insert(student);
    }

    @Override
    public void applyJoin(Newcomer newComer) {
        Integer studentId = newComer.getStudentId();
        if (newcomerMapper.selectByStudentId(studentId) != null)
            throw new RepeatRuestException(MessageConstant.REPEATREQUEST);

        newComer.setApplicationDateTime(LocalDateTime.now());

        newcomerMapper.insert(newComer);
    }

    @Override
    public List<NewcomerVO> getAll() {
        List<Newcomer> list = this.list();
        List<NewcomerVO> newcomerVOS = new ArrayList<>();
        for (Newcomer newcomer : list) {
            NewcomerVO newcomerVO = new NewcomerVO();
            BeanUtils.copyProperties(newcomer, newcomerVO);
            newcomerVOS.add(newcomerVO);
        }

        return newcomerVOS;
    }
}
