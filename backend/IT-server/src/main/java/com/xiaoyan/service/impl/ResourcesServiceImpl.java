package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.xiaoyan.mapper.ResourcesMapper;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.PermissionService;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static com.xiaoyan.constant.RedisConstant.CACHE_RESOURCES;
import static com.xiaoyan.constant.RedisConstant.CACHE_RESOURCES_ALL;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS_ALL;


/**
 * @author yuchao
 */
@Service
@AllArgsConstructor
public class ResourcesServiceImpl extends ServiceImpl<ResourcesMapper, Resources>
        implements ResourcesService {

    private final PermissionService permissionService;
    private ResourcesMapper resourcesMapper;
    private StringRedisTemplate stringRedisTemplate;
    private UsersService usersService;
    private RedisUtil redisUtil;
    private CommonService commonService;

    private StudentFileMapper studentFileMapper;

    @Override
    public Long getCount() {
        return this.count();
    }

    @Override
    public List<ResourcesVO> getList(Integer studentId) {
        List<ResourcesVO> list = redisUtil.getAllWithHashCache(CACHE_RESOURCES, resourcesMapper::selectResourcesWithDetails,
                ResourcesVO.class);
        if (studentId != null) {
            list.removeIf(vo->!studentId.equals(vo.getStudentId()));
        }
        return list;
    }

    @Override
    public void saveResource(ResourcesDTO resourcesDTO, Integer studentId) throws IOException {
        StudentFile cover = commonService.upload(resourcesDTO.getCover());
        StudentFile file = commonService.upload(resourcesDTO.getFile());

        Resources resource = Resources.builder().
                head(resourcesDTO.getHead()).
                introduce(resourcesDTO.getIntroduce()).
                studentId(studentId).
                studentFileCoverId(cover.getId()).
                studentFileFileId(file.getId()).
                releaseDateTime(LocalDateTime.now()).build();

        resourcesMapper.insert(resource);
        stringRedisTemplate.delete(CACHE_RESOURCES_ALL);
        stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
        StudentVO author = usersService.getUser(studentId);

        ResourcesVO vo = BeanUtil.toBean(resource, ResourcesVO.class);
        vo.setStudentName(author.getName());
        vo.setAvatar(author.getAvatar());
        vo.setFileUrl(file.getFileUrl());
        vo.setFileName(file.getOriginalName());
        vo.setCoverUrl(cover.getFileUrl());

        stringRedisTemplate.opsForHash().put(CACHE_RESOURCES, String.valueOf(resource.getId()),
                JSONUtil.toJsonStr(vo));

    }

    @Override
    public void deleteById(Long id, Integer studentId) {
        Resources resource = getById(id);

        permissionService.checkOwnerOrAdminPermission(resource.getStudentId());

        StudentFile file = studentFileMapper.selectById(resource.getStudentFileFileId());
        commonService.delete(file.getObjectName());
        StudentFile cover = studentFileMapper.selectById(resource.getStudentFileCoverId());
        commonService.delete(cover.getObjectName());

        resourcesMapper.deleteById(id);
        stringRedisTemplate.opsForHash().delete(CACHE_RESOURCES, String.valueOf(id));
        stringRedisTemplate.delete(CACHE_RESOURCES_ALL);
        stringRedisTemplate.delete(CACHE_STUDENTS_ALL);

    }


}
