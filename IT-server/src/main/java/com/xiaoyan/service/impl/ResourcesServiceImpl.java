package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.annotation.AutoFillFields;
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

    private CommonService commonService;

    private StudentFileMapper studentFileMapper;

    @Override
    public Long getCount() {
        return this.count();
    }

    @Override
    public List<ResourcesVO> getList() {
        List<Resources> resources = resourcesMapper.selectList(null);
        List<ResourcesVO> list = new ArrayList<>();
        for (Resources resource : resources) {
            Integer studentFileCoverId = resource.getStudentFileCoverId();
            Integer studentFileFileId = resource.getStudentFileFileId();
            Integer studentId = resource.getStudentId();

            ResourcesVO resourcesVO = new ResourcesVO();
            resourcesVO.setCoverUrl(studentFileMapper.selectById(studentFileCoverId).getFileUrl());
            StudentFile file = studentFileMapper.selectById(studentFileFileId);
            resourcesVO.setFileUrl(file.getFileUrl());
            resourcesVO.setFileName(file.getOriginalName());
            resourcesVO.setStudentName(userMapper.selectByStudentId(studentId).getName());
            resourcesVO.setObjectName(file.getObjectName());

            BeanUtils.copyProperties(resource, resourcesVO);
            list.add(resourcesVO);
        }

        list.sort((o1, o2) -> o2.getReleaseDateTime().compareTo(o1.getReleaseDateTime()));

        return list;
    }

    @Override
    @AutoFillFields(AutoFillFields.OpType.INSERT)
    public void saveResource(ResourcesDTO resourcesDTO, Integer studentId) {
        try {
            Integer coverId = commonService.upload(resourcesDTO.getCover());
            Integer fileId = commonService.upload(resourcesDTO.getFile());

            resourcesMapper.insert(Resources.builder().
                    head(resourcesDTO.getHead()).
                    introduce(resourcesDTO.getIntroduce()).
                    studentId(studentId).
                    studentFileCoverId(coverId).
                    studentFileFileId(fileId).
                    releaseDateTime(LocalDateTime.now()).build());

            userMapper.addReourceCountByID(studentId);
        } catch (Exception e) {
            log.error("保存资源失败：", e); // 详细打印异常栈
            // 抛出自定义业务异常，让Controller可以捕获并返回更友好的错误信息
            throw new RuntimeException("资源上传或保存失败，请稍后再试", e);
        }
    }

    @Override
    public void deleteById(Integer id, Integer studentId) {
        Resources resources = resourcesMapper.selectById(id);
        if (resources == null)
            throw new ParameterException(MessageConstant.RRSOURCES_NO_EXISITS);

        if (!resources.getStudentId().equals(studentId))
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);


        StudentFile cover = studentFileMapper.selectById(resources.getStudentFileCoverId());
        commonService.delete(cover.getObjectName());
        StudentFile file = studentFileMapper.selectById(resources.getStudentFileFileId());
        commonService.delete(file.getObjectName());

        resourcesMapper.deleteById(id);

        userMapper.decreaceResourceCount(studentId);
    }


}
