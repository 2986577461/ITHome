package com.xiaoyan.service.impl;

import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.utils.AliOssUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
@AllArgsConstructor
public class CommonServiceImpl implements CommonService {

    private AliOssUtil aliOssUtil;

    private StudentFileMapper studentFileMapper;

    @Override
    public Integer upload(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null)
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);

        String suffix = originalName.substring(originalName.lastIndexOf("."));

        String objectName = UUID.randomUUID() + suffix;
        String fileUrl = aliOssUtil.upload(file.getBytes(), objectName);

        long size = file.getSize();
        String contentType = file.getContentType();

        StudentFile record = StudentFile.builder().
                studentId(BaseContext.getCurrentStudentId()).
                fileSize(size).
                originalName(originalName).
                objectName(objectName).
                fileType(contentType).
                createDateTime(LocalDateTime.now()).
                fileUrl(fileUrl).build();

        studentFileMapper.insert(record);

        //回显id
        return record.getId();
    }

    @Override
    public void delete(String objectName) {
        if (!aliOssUtil.delete(objectName))
            throw new RuntimeException(MessageConstant.FILE_DELETE_FAILED);
        studentFileMapper.deleteByObjectName(objectName);
    }
}
