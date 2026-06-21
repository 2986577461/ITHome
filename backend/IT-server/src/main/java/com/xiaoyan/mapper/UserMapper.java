package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.xiaoyan.pojo.Student;

import java.util.List;
import java.util.Set;

@Mapper
public interface UserMapper extends BaseMapper<Student> {

    @Select("select * from it_student where student_id=#{studentId} and deleted=0")
    Student selectByStudentId(Integer studentId);

    List<Student> selectByStudentIds(@Param("studentIds") Set<Integer> studentIds);

    void deletebyStudentIds(List<String> studentIds);

    Set<String> selectPositionByIds(List<Integer> studentIds);

    List<Student> selectThisYearsStudents();

}