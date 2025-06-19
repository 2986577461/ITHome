package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.annotation.AutoFillFields;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.mapper.ResourcesMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.vo.ResourcesVO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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
    @AutoFillFields(AutoFillFields.OpType.INSERT)
    public void upload(@Valid Resources resources) {
        resourcesMapper.insert(resources);

        userMapper.addReourceCountByID(BaseContext.getCurrentId());
    }


}
