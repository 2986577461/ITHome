package com.xiaoyan.service;

import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.vo.NewcomerVO;
import jakarta.validation.Valid;

import java.util.List;

public interface NewcomersService {

    void refuseNewcomer(Long id);

    void agreeNewcomer(Long id);

    void applyJoin(@Valid Newcomer newComer);

    List<NewcomerVO> getAll();

}
