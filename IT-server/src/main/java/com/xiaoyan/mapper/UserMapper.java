package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<Student> {

    @Update("update it_student set " +
            "resource_count=resource_count+1 where student_id=#{studentId}")
    void addReourceCountByID(Integer studentId);

    @Update("update it_student set password=#{password} where student_id=#{studentId}")
    void updatePasswordByStudentId(Integer studentId, String password);


    @Select("select * from it_student where student_id=#{studentId}")
    Student selectByStudentId(Integer studentId);

}
