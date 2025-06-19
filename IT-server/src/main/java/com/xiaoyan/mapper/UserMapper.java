package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.Student;
import jakarta.validation.constraints.NotBlank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<Student> {

    @Select("update it_student set " +
            "resource_count=resource_count+1 where id=#{id}")
    void addReourceCountByID(Long id);

    @Update("update it_student set password=#{password} where id=#{id}")
    void updatePasswordById(Long id, @NotBlank String password);
}
