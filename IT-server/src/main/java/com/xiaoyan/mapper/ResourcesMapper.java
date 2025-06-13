package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.Resources;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourcesMapper extends BaseMapper<Resources> {

    String selectFileNameById(int id);
}
