package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.vo.StudentCountVO;

import java.util.List;

@Mapper
public interface ResourcesMapper extends BaseMapper<Resources> {

    int selectCountByStudentId(String studentId);

    /** 一次查出多个学生的资料数，替代在循环里逐个 count */
    List<StudentCountVO> countByStudentIds(@Param("studentIds") List<String> studentIds);

    List<Resources> selectByStudentIds(@Param("studentIds") List<String> studentIds);

    int deleteByStudentIds(@Param("studentIds") List<String> studentIds);
}
