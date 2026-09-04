package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.vo.ResourcesVO;

import java.util.List;

@Mapper
public interface ResourcesMapper extends BaseMapper<Resources> {
    List<ResourcesVO> selectResourcesWithDetails();

    int selectCountByStudentId(Integer studentId);
}
