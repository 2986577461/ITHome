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

    @Update("update it_student set resource_count=resource_count+1 where student_id=#{studentId}")
    void addReourceCountByID(Integer studentId);

    @Update("update it_student set article_count=article_count+1 where student_id=#{studentId}")
    void addArticleCountById(Integer studentId);

    @Update("update it_student set article_count=article_count-1 where student_id=#{studentId}")
    void decreaceArticleCount(Integer studentId);

    @Update("update it_student set resource_count=resource_count-1 where student_id=#{studentId}")
    void decreaceResourceCount(Integer studentId);

    @Select("select name from it_student where student_id=#{studentId};")
    String selectNameByStudentId(Integer studentId);

    List<Student> selectByStudentIds(@Param("studentIds") Set<Integer> studentIds);
}