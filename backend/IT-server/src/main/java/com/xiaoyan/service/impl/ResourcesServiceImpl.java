package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ResourcesMapper;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.vo.StudentVO;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.xiaoyan.dto.ResourcesDTO;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.vo.ResourcesVO;

import java.time.LocalDateTime;
import java.util.List;

import static com.xiaoyan.constant.RedisConstant.CACHE_COUNT_RESOURCES;
import static com.xiaoyan.constant.RedisConstant.CACHE_RESOURCES;
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
    private UsersService usersService;
    private RedisUtil redisUtil;
    private CommonService commonService;

    private StudentFileMapper studentFileMapper;

    @Override
    public Long getCount() {
        return this.count();
    }

    @Override
    public List<ResourcesVO> getList() {
        List<ResourcesVO> list = redisUtil.getAllWithHashCache(CACHE_RESOURCES, this::count, this::queryResourcesByDB,
                ResourcesVO.class);

        return list.stream().toList();
    }

    private List<ResourcesVO> queryResourcesByDB() {
        List<Resources> list = this.list();

        return list.stream().map(r -> {
            ResourcesVO rvo = BeanUtil.toBean(r, ResourcesVO.class);

            StudentFile cover = studentFileMapper.selectById(r.getStudentFileCoverId());
            if (cover != null) {
                rvo.setCoverUrl(cover.getFileUrl());
            }
            StudentFile file = studentFileMapper.selectById(r.getStudentFileFileId());
            if (file != null) {
                rvo.setFileUrl(file.getFileUrl());
                rvo.setFileName(file.getOriginalName());
                rvo.setObjectName(file.getObjectName());
            }
            StudentVO vo = usersService.getUser(r.getStudentId());
            if (vo != null) {
                rvo.setAvatar(vo.getAvatar());
                rvo.setStudentName(vo.getName());
            }
            return rvo;
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