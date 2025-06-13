package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.NewcomerMapper;

import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.ITStudent;
import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.service.NewcomersService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class NewcomersServiceImpl extends ServiceImpl<NewcomerMapper, Newcomer>
        implements NewcomersService {

    private NewcomerMapper newcomerMapper;

    private UserMapper userMapper;

    @Override
    public void refuseNewcomer(int id) {
        this.removeById(id);
    }

    @Override
    @Transactional // 开启事务
    public void agreeNewcomer(int id) {
        Newcomer newcomer = newcomerMapper.selectById(id);
        if (newcomer == null)
            throw new ParameterException("参数异常");

        if (userMapper.selectById(newcomer.getStudentId()) != null)
            throw new RuntimeException("学员重复申请！");

        newcomerMapper.deleteById(id);

        userMapper.insert(new ITStudent(newcomer));
    }

    @Override
    public void applyJoin(Newcomer newcomer) {
        if (userMapper.selectById(newcomer.getStudentId()) == null)
            this.save(newcomer);
    }

    @Override
    public List<Newcomer> getAllnewcomer() {
        return  newcomerMapper.selectList(null);
    }

}
