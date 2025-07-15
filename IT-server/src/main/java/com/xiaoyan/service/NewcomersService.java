package com.xiaoyan.service;

import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.vo.NewcomerVO;

import java.util.List;

public interface NewcomersService {

    void refuseNewcomer(Long id);

    void agreeNewcomer(Long id);

    void applyJoin(Newcomer newComer);

    List<NewcomerVO> getAll();

}
