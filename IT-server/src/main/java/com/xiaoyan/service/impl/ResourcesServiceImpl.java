package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.ResourceDTO;
import com.xiaoyan.exception.AccountNotFoundException;
import com.xiaoyan.exception.PositionException;
import com.xiaoyan.mapper.ResourcesMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.vo.ResourcesVO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@AllArgsConstructor
public class ResourcesServiceImpl extends ServiceImpl<ResourcesMapper, Resources>
        implements ResourcesService {

    private ResourcesMapper resourcesMapper;

    private UserMapper userMapper;

    @Override
    public Long getCount() {
        return this.count();
    }

    @Override
    public List<ResourcesVO> getList() {
        List<Resources> resources = resourcesMapper.selectList(null);
        List<ResourcesVO> list = new ArrayList<>();
        for (Resources resource : resources) {
            ResourcesVO resourcesVO = new ResourcesVO();
            BeanUtils.copyProperties(resource, resourcesVO);
            list.add(resourcesVO);
        }
        return list;
    }


    @Override
    public void upload(String resourceJson) {
        ObjectMapper objectMapper = new ObjectMapper();

        ResourceDTO resourceDTO=null;
        try {
            resourceDTO = objectMapper.readValue(resourceJson, ResourceDTO.class);
        }catch ( JsonProcessingException ex){
            ex.printStackTrace();
        }

        if (resourceDTO.getStudentId()!=BaseContext.getCurrentId())
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);

        Student student = userMapper.selectById(resourceDTO.getStudentId());

        if (!resourceDTO.getStudentName().equals(student.getName()))
            throw new PositionException(MessageConstant.NAME_MISMATCH);

        Resources resources = new Resources();
        BeanUtils.copyProperties(resourceDTO, resources);
        resources.setReleaseDateTime(LocalDateTime.now());

        resourcesMapper.insert(resources);

        userMapper.addReourceCountByID(BaseContext.getCurrentId());
    }


}
