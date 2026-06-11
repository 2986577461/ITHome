package com.xiaoyan.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.xiaoyan.pojo.StudentFile;

import java.util.List;
import java.util.Set;

@Mapper
public interface StudentFileMapper extends BaseMapper<StudentFile> {

    /** 批量软删除 student_file */
    @Update("<script>" +
            "update student_file set deleted=1 where object_name in " +
            "<foreach collection='objectNames' item='name' open='(' separator=',' close=')'>" +
            "#{name}" +
            "</foreach>" +
            " and deleted=0" +
            "</script>")
    void deleteByObjectNames(@Param("objectNames") List<String> objectNames);

    @Select("select * from student_file where object_name=#{objectName} and deleted=0;")
    StudentFile selectbyObjectName(String objectName);
}