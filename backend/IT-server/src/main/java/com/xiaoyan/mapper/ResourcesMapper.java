package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.vo.MyResourceVO;
import com.xiaoyan.vo.ResourcesVO;
import com.xiaoyan.vo.StudentCountVO;

import java.util.List;

@Mapper
public interface ResourcesMapper extends BaseMapper<Resources> {

    /** 资料列表，连同封面、附件、作者姓名和头像一次查出，避免逐条回查造成的 N+1 */
    List<ResourcesVO> selectAllWithDetail();

    /** 某个人的资料列表，只取「我的资料」页需要的字段 */
    List<MyResourceVO> selectMyResources(@Param("studentId") String studentId);

    /** 一次查出多个学生的资料数，替代在循环里逐个 count */
    List<StudentCountVO> countByStudentIds(@Param("studentIds") List<String> studentIds);
}