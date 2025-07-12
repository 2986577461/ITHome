package com.xiaoyan.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.StudentFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StudentFileMapper extends BaseMapper<StudentFile> {

    @Update("update student_file set deleted=1 where object_name=#{objectName} and deleted=0")
    void deleteByObjectName(String objectName);

    @Select("select * from student_file where object_name=#{objectName} and deleted=0;")
    StudentFile selectbyObjectName(String objectName);
}
