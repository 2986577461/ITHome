package com.xiaoyan.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.StudentMedals;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MedalsMapper extends BaseMapper<StudentMedals> {

    List<StudentMedals> selectByStudentId(Integer studentId);
}
