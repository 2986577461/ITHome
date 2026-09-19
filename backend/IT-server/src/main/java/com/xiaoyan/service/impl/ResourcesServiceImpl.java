package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.ResourcesMapper;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.AsyncExecutors;
import com.xiaoyan.utils.RedisUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.xiaoyan.dto.ResourcesDTO;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.vo.MyResourceVO;
import com.xiaoyan.vo.ResourcesVO;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static com.xiaoyan.constant.RedisConstant.CACHE_RESOURCES_ALL;


/**
 * @author yuchao
 */
@Slf4j
@Service
@AllArgsConstructor
public class ResourcesServiceImpl extends ServiceImpl<ResourcesMapper, Resources>
        implements ResourcesService {

    private ResourcesMapper resourcesMapper;
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
        return redisUtil.queryStringWithMutex(CACHE_RESOURCES_ALL, ResourcesVO.class,
                resourcesMapper::selectAllWithDetail);
    }

    @Override
    public List<MyResourceVO> getMyResources(String studentId) {
        return resourcesMapper.selectMyResources(studentId);
    }

    @Override
    public void saveResource(ResourcesDTO resourcesDTO, String studentId) throws IOException {
        CopiedFile coverFile = copyFile(resourcesDTO.getCover());
        CopiedFile resourceFile = copyFile(resourcesDTO.getFile());
        String head = resourcesDTO.getHead();
        String introduce = resourcesDTO.getIntroduce();
        LocalDateTime releaseDateTime = LocalDateTime.now();

        // 走全局共享线程池，不再每次上传裸起一个 Thread。
        // 池内队列满时 CallerRunsPolicy 会让任务退回调用线程执行，宁可拖慢这次请求也不丢任务。
        AsyncExecutors.uploadExecutor().execute(() -> {
            try {
                StudentFile cover = commonService.upload(coverFile.bytes(), coverFile.originalName(),
                        coverFile.contentType(), coverFile.size(), studentId);
                StudentFile file = commonService.upload(resourceFile.bytes(), resourceFile.originalName(),
                        resourceFile.contentType(), resourceFile.size(), studentId);

                Resources resource = Resources.builder().
                        head(head).
                        introduce(introduce).
                        studentId(studentId).
                        studentFileCoverId(cover.getId()).
                        studentFileFileId(file.getId()).
                        releaseDateTime(releaseDateTime).build();

                resourcesMapper.insert(resource);
                redisUtil.evict(CACHE_RESOURCES_ALL);
            } catch (Exception e) {
                log.error("异步上传资料失败, studentId={}", studentId, e);
            }
        });
    }

    @Override
    public void deleteById(Long id, String studentId) {
        Resources resource = getById(id);
        if (resource == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        usersService.checkOwnerOrAdmin(resource.getStudentId());

        StudentFile file = studentFileMapper.selectById(resource.getStudentFileFileId());
        if (file != null) {
            commonService.delete(file.getObjectName());
        }
        StudentFile cover = studentFileMapper.selectById(resource.getStudentFileCoverId());
        if (cover != null) {
            commonService.delete(cover.getObjectName());
        }

        resourcesMapper.deleteById(id);
        redisUtil.evict(CACHE_RESOURCES_ALL);

    }

    private CopiedFile copyFile(MultipartFile file) throws IOException {
        if (file == null || file.getOriginalFilename() == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }
        return new CopiedFile(file.getBytes(), file.getOriginalFilename(), file.getContentType(), file.getSize());
    }

    private record CopiedFile(byte[] bytes, String originalName, String contentType, long size) {
    }

}