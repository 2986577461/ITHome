package com.xiaoyan.service;

import com.xiaoyan.pojo.Newcomer;

import java.util.List;

public interface NewcomersService {

    void refuseNewcomer(Long id);

    void agreeNewcomer(Long id);

    void applyJoin(Newcomer newcomer);

    List<Newcomer> getAll();


}
