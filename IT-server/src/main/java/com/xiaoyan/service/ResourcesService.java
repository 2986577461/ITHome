package com.xiaoyan.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.vo.ResourcesVO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ResourcesService extends IService<Resources> {

    Long getCount();

    void upload(@Valid Resources resources);

    List<ResourcesVO> getList();

}
