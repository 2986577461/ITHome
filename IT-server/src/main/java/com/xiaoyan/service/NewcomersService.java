package com.xiaoyan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.vo.NewcomerVO;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface NewcomersService extends IService<Newcomer> {

    void agreeNewcomer(Long id);

    void applyJoin(Newcomer newComer);

    List<NewcomerVO> getAll();

    void refuseNewcomer(@NotNull Long id);
}