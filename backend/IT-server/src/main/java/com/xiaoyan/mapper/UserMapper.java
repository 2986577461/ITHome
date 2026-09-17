package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.xiaoyan.pojo.Student;

import java.util.List;
import java.util.Set;

@Mapper
public interface UserMapper extends BaseMapper<Student> {

    @Select("select * from it_student where student_id=#{studentId} and deleted=0")
    Student selectByStudentId(String studentId);

    /** 统计某个职位的学生数，用于判断「最后一个管理员」 */
    @Select("select count(*) from it_student where position=#{position} and deleted=0")
    int countByPosition(@Param("position") String position);

    List<Student> selectByStudentIds(@Param("studentIds") Set<String> studentIds);

    void deletebyStudentIds(List<String> studentIds);

    Set<String> selectPositionByIds(List<String> studentIds);

    List<Student> selectThisYearsStudents();

}
