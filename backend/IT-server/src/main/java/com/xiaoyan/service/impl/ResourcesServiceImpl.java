package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ResourcesMapper;
import com.xiaoyan.mapper.StudentFileMapper;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static com.xiaoyan.constant.RedisConstant.CACHE_RESOURCES;


/**
 * @author yuchao
 */
@Service
@AllArgsConstructor
public class ResourcesServiceImpl extends ServiceImpl<ResourcesMapper, Resources>
        implements ResourcesService {

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
        StudentVO user = usersService.getUser(studentId);


        if (!JwtClaimsConstant.ADMIN_ID.equals(user.getPosition()) && !studentId.equals(resource.getStudentId())) {
            throw new ParameterException(MessageConstant.PERMISSION_DENIED);
        }

        StudentFile file = studentFileMapper.selectById(resource.getStudentFileFileId());
        commonService.delete(file.getObjectName());
        StudentFile cover = studentFileMapper.selectById(resource.getStudentFileCoverId());
        commonService.delete(cover.getObjectName());

        resourcesMapper.deleteById(id);
        stringRedisTemplate.opsForHash().delete(CACHE_RESOURCES, String.valueOf(id));

    }


}