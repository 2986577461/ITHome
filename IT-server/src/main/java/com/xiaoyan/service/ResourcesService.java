package com.xiaoyan.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.dto.ResourcesDTO;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.vo.ResourcesVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ResourcesService  extends IService<Resources> {

    Long getCount();

    List<ResourcesVO> getList();

    void saveResource(ResourcesDTO resourcesDTO,Integer studentId);

    void deleteById(Long id,Integer studentId);
}