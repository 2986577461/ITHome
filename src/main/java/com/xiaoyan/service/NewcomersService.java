package com.xiaoyan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.pojo.Newcomer;

import java.util.ArrayList;

public interface NewcomersService extends IService<Newcomer> {

    boolean refuseNewcomer(int id);

    boolean agreeNewcomer(int id);

    boolean applyJoin(Newcomer newcomer);

    ArrayList<Newcomer> getAllnewcomer();


}
