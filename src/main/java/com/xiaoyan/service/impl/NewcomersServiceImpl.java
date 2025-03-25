package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.NewcomerMapper;

import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.ITStudent;
import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.service.NewcomersService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class NewcomersServiceImpl extends ServiceImpl<NewcomerMapper,Newcomer>
        implements NewcomersService {

    @Resource
    private NewcomerMapper newcomerMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public boolean refuseNewcomer(int id) {
       return this.removeById(id);
    }

    @Override
    @Transactional // 开启事务
    public boolean agreeNewcomer(int id)  {
        Newcomer newcomer = newcomerMapper.selectById(id);
        if (newcomer == null)
            throw new ParameterException("参数异常");

        if(userMapper.selectById(newcomer.getStudentId())!=null)
            throw new RuntimeException("学员重复申请！");

        newcomerMapper.deleteById(id);

        return userMapper.insert(new ITStudent(newcomer))==1;
    }

    @Override
    public boolean applyJoin(Newcomer newcomer) {
        if(userMapper.selectById(newcomer.getStudentId())==null)
            return this.save(newcomer);
       return false;

    }

    @Override
    public ArrayList<Newcomer> getAllnewcomer() {
        return (ArrayList<Newcomer>) newcomerMapper.selectList(null);
    }

}
