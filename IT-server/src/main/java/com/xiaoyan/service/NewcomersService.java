package com.xiaoyan.service;

import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.vo.NewcomerVO;

import java.util.List;

public interface NewcomersService {

    void refuseNewcomer(Integer id);

    void agreeNewcomer(Integer id);

    void applyJoin(Newcomer newComer);

    List<NewcomerVO> getAll();

}
