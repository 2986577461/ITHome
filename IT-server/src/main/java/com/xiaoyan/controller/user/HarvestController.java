package com.xiaoyan.controller.user;


import com.xiaoyan.vo.HarvestVO;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.HarvestService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("user/harvest")
@AllArgsConstructor
public class HarvestController {

    private HarvestService harvestService;

    @GetMapping("all")
    public Result<List<HarvestVO>> getAll() {
        List<HarvestVO> harvestVO = harvestService.getAll();
        return Result.success(harvestVO);
    }
}
