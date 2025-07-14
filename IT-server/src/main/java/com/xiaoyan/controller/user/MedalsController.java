package com.xiaoyan.controller.user;

import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.StudentMedalsDTO;
import com.xiaoyan.enumeration.MedalsGradeType;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.MedalsService;
import com.xiaoyan.vo.StudentMedalsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("user/medals")
@Slf4j
@AllArgsConstructor
@Tag(name = "奖项管理")
public class MedalsController {

    private MedalsService medalsService;

    @GetMapping("all")
    @Operation(summary = "获取所有奖项")
    @Cacheable(value = "allMedals",key = "'allMedals'")
    public Result<List<StudentMedalsVO>> getAll() {
        log.info("获取所有奖项");
        List<StudentMedalsVO> medalsVO= medalsService.getAll();
        return Result.success(medalsVO);
    }

    @GetMapping
    @Operation(summary = "获取当前用户奖项")
    @Cacheable(value = "currentUserMedals",key = "'currentUserMedals'")
    public Result<List<StudentMedalsVO>> getCurrentUserMedals() {
        Integer studentId = BaseContext.getCurrentStudentId();
        log.info("获取用户{}的奖项",studentId);
        List<StudentMedalsVO> currentUserMedals = medalsService.getUserMedals(studentId);
        return Result.success(currentUserMedals);
    }

    @PostMapping
    @Operation(summary = "上传奖项")
    @CacheEvict(cacheNames = {"currentUserMedals","allMedals"},allEntries = true)
    public Result<String> save(@ModelAttribute @Valid StudentMedalsDTO medalsDTO) throws IOException {
        log.info("上传奖项:{}",medalsDTO);
        log.info("上传奖项:{}{}",medalsDTO.getHead(), MedalsGradeType.fromCode(medalsDTO.getGrade()).getDescription());
        medalsService.save(medalsDTO);
        return Result.success();
    }

    @DeleteMapping("{id}")
    @Operation(summary = "给定id删除自己的奖项")
    @CacheEvict(cacheNames = {"currentUserMedals","allMedals"},allEntries = true)
    public Result<String> remove(@PathVariable Integer id) {
        Integer studentId = BaseContext.getCurrentStudentId();
        log.info("用户{}删除了奖项{}",studentId,id);
        medalsService.remove(id, studentId);
        return Result.success();
    }
}
