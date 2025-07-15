package com.xiaoyan.service;


import com.xiaoyan.dto.ResourcesDTO;
import com.xiaoyan.vo.ResourcesVO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public interface ResourcesService  {

    Long getCount();

    List<ResourcesVO> getList();

    void saveResource(ResourcesDTO resourcesDTO,Integer studentId);

    void deleteById(Integer id,Integer studentId);
}
