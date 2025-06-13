package com.xiaoyan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.pojo.Newcomer;

import java.util.List;

public interface NewcomersService extends IService<Newcomer> {

    void refuseNewcomer(int id);

    void agreeNewcomer(int id);

    void applyJoin(Newcomer newcomer);

    List<Newcomer> getAllnewcomer();


}
