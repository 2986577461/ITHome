package com.xiaoyan.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import com.xiaoyan.dto.ResourcesDTO;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.vo.ResourcesVO;

import java.io.IOException;
import java.util.List;

@Service
public interface ResourcesService extends IService<Resources> {

    Long getCount();

    List<ResourcesVO> getList(Integer studentId);

    void saveResource(ResourcesDTO resourcesDTO, Integer studentId) throws IOException;

    void deleteById(Long id,Integer studentId);
}