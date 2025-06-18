package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<Student> {

    @Select("update it_student set " +
            "resource_count=resource_count+1 where id=#{id}")
    void addReourceCountByID(Long id);

}
