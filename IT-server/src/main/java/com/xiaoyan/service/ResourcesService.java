package com.xiaoyan.service;


import com.xiaoyan.pojo.Resources;
import com.xiaoyan.vo.ResourcesVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ResourcesService  {

    Long getCount();


    List<ResourcesVO> getList();

    void saveResource(Resources resources);
}
