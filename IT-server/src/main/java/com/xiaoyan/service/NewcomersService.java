package com.xiaoyan.service;

import com.xiaoyan.dto.NewComerDTO;
import com.xiaoyan.pojo.Newcomer;

import java.util.List;

public interface NewcomersService {

    void refuseNewcomer(Long id);

    void agreeNewcomer(Long id);

    void applyJoin(NewComerDTO newComerDTO);

    List<Newcomer> getAll();


}
