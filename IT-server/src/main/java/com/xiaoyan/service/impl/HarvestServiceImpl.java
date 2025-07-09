package com.xiaoyan.service.impl;


import com.xiaoyan.mapper.HarvestMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Harvest;
import com.xiaoyan.service.HarvestService;
import com.xiaoyan.vo.HarvestVO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class HarvestServiceImpl implements HarvestService {

    private HarvestMapper harvestMapper;

    private UserMapper userMapper;

    @Override
    public List<HarvestVO> getAll() {
        List<Harvest> harvests = harvestMapper.selectList(null);
        List<HarvestVO> harvestVOS = new ArrayList<>();
        for (Harvest harvest : harvests) {
            HarvestVO harvestVO = new HarvestVO();
            BeanUtils.copyProperties(harvest, harvestVO);
            harvestVOS.add(harvestVO);
        }
        return harvestVOS;
    }
}
