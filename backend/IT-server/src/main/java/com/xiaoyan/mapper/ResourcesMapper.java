package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xiaoyan.pojo.Resources;

import java.util.List;

@Mapper
public interface ResourcesMapper extends BaseMapper<Resources> {

    int selectCountByStudentId(String studentId);

    List<Resources> selectByStudentIds(@Param("studentIds") List<String> studentIds);

    int deleteByStudentIds(@Param("studentIds") List<String> studentIds);
}
