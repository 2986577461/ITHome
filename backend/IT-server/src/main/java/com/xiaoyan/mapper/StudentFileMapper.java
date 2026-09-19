package com.xiaoyan.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.xiaoyan.pojo.StudentFile;

import java.util.List;

@Mapper
public interface StudentFileMapper extends BaseMapper<StudentFile> {

    void deleteByObjectNames(@Param("objectNames") List<String> objectNames);

    @Select("select * from student_file where object_name=#{objectName};")
    StudentFile selectbyObjectName(String objectName);
}