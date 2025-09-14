package com.xiaoyan.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.dto.ResourcesDTO;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ResourcesMapper;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.vo.ResourcesVO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.xiaoyan.constant.RedisConstant.CACHE_RESOURCES;
import static com.xiaoyan.constant.RedisConstant.CACHE_COUNT_RESOURCES;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS;


/**
 * @author yuchao
 */
@Service
@AllArgsConstructor
public class ResourcesServiceImpl extends ServiceImpl<ResourcesMapper, Resources>
        implements ResourcesService {

    private ResourcesMapper resourcesMapper;
    private StringRedisTemplate stringRedisTemplate;
    private UserMapper userMapper;
    private RedisUtil redisUtil;
    private CommonService commonService;

    private StudentFileMapper studentFileMapper;

    @Override
    public Long getCount() {
        return redisUtil.queryCountWithLogicalExpire(CACHE_COUNT_RESOURCES, this::count);
    }

    @Override
    public List<ResourcesVO> getList() {
        List<Resources> list = redisUtil.getAllWithHashCache(CACHE_RESOURCES, this::count, query()::list,
                Resources.class, Resources.class);

        return list.stream().map(r -> {
            Long studentFileCoverId = r.getStudentFileCoverId();
            Long studentFileFileId = r.getStudentFileFileId();
            Integer studentId = r.getStudentId();

            ResourcesVO resourcesVO = new ResourcesVO();
            resourcesVO.setCoverUrl(studentFileMapper.selectById(studentFileCoverId).getFileUrl());
            StudentFile file = studentFileMapper.selectById(studentFileFileId);
            resourcesVO.setFileUrl(file.getFileUrl());
            resourcesVO.setFileName(file.getOriginalName());
            resourcesVO.setStudentName(userMapper.selectNameByStudentId(studentId));
            resourcesVO.setObjectName(file.getObjectName());

            BeanUtils.copyProperties(r, resourcesVO);
            return resourcesVO;
        }).toList();
    }

    @Override
    public void saveResource(ResourcesDTO resourcesDTO, Integer studentId) {
        try {
            Long coverId = commonService.upload(resourcesDTO.getCover());
            Long fileId = commonService.upload(resourcesDTO.getFile());

            Resources resources = Resources.builder().
                    head(resourcesDTO.getHead()).
                    introduce(resourcesDTO.getIntroduce()).
                    studentId(studentId).
                    studentFileCoverId(coverId).
                    studentFileFileId(fileId).
                    releaseDateTime(LocalDateTime.now()).build();

            resourcesMapper.insert(resources);
            stringRedisTemplate.opsForHash().put(CACHE_RESOURCES,
                    String.valueOf(resources.getId()), JSONUtil.toJsonStr(resources));

            userMapper.addReourceCountByID(studentId);
            stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(studentId));
            stringRedisTemplate.delete(CACHE_COUNT_RESOURCES);
        } catch (Exception e) {
            // 详细打印异常栈
            log.error("保存资源失败：", e);
            // 抛出自定义业务异常，让Controller可以捕获并返回更友好的错误信息
            throw new RuntimeException("资源上传或保存失败，请稍后再试", e);
        }
    }

    @Override
    public void deleteById(Long id, Integer studentId) {
        Object o = stringRedisTemplate.opsForHash().get(CACHE_RESOURCES, String.valueOf(id));
        if (o == null) {
            throw new ParameterException(MessageConstant.RRSOURCES_NO_EXISITS);
        }
        Resources resources = JSONUtil.toBean((String) o, Resources.class);
        if (!resources.getStudentId().equals(studentId)) {
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);
        }

        StudentFile cover = studentFileMapper.selectById(resources.getStudentFileCoverId());
        commonService.delete(cover.getObjectName());
        StudentFile file = studentFileMapper.selectById(resources.getStudentFileFileId());
        commonService.delete(file.getObjectName());

        resourcesMapper.deleteById(id);
        stringRedisTemplate.opsForHash().delete(CACHE_RESOURCES, String.valueOf(id));

        userMapper.decreaceResourceCount(studentId);
        stringRedisTemplate.delete(CACHE_COUNT_RESOURCES);
    }


}