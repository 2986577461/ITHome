package com.xiaoyan.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.annotation.AutoFillFields;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.mapper.ResourcesMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.service.ResourcesService;
import com.xiaoyan.utils.AliOssUtil;
import com.xiaoyan.vo.ResourcesVO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
@AllArgsConstructor
public class ResourcesServiceImpl extends ServiceImpl<ResourcesMapper, Resources>
        implements ResourcesService {

    private ResourcesMapper resourcesMapper;

    private UserMapper userMapper;

    @Override
    public Long getCount() {
        return this.count();
    }

    @Override
    public List<ResourcesVO> getList() {
        List<Resources> resources = resourcesMapper.selectList(null);
        List<ResourcesVO> list = new ArrayList<>();
        for (Resources resource : resources) {
            ResourcesVO resourcesVO = new ResourcesVO();
            BeanUtils.copyProperties(resource, resourcesVO);
            list.add(resourcesVO);
        }
        return list;
    }

    @Override
    @AutoFillFields(AutoFillFields.OpType.INSERT)
    public void saveResource(Resources resources) {
        resources.setStudentId(BaseContext.getCurrentStudentId());
        resources.setReleaseDateTime(LocalDateTime.now());
        resourcesMapper.insert(resources);
        userMapper.addReourceCountByID(BaseContext.getCurrentStudentId());
    }

    @Override
    public void deleteById(String id) {
        resourcesMapper.deleteById(id);
    }

    private AliOssUtil aliOssUtil;

    /**
     * 生成带签名的下载URL
     *
     * @param objectName       OSS上的文件路径/名称
     * @param friendlyFileName 你希望用户下载时看到的文件名
     * @param expirationMillis URL的过期时间（毫秒），例如 3600 * 1000 = 1小时
     * @return 带签名的下载URL
     */
    public String generatePresignedDownloadUrl(String objectName, String friendlyFileName, long expirationMillis) {
        // 创建GeneratePresignedUrlRequest对象
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(aliOssUtil.getBucketName(), objectName);

        // 设置签名URL的有效时间
        Date expiration = new Date(System.currentTimeMillis() + expirationMillis);
        request.setExpiration(expiration);

        // 设置Content-Disposition，指定下载的文件名
        ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();
        // 对文件名进行URL编码，并符合RFC 6266标准
        String encodedFriendlyName = new String(friendlyFileName.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.ISO_8859_1);
        responseHeaders.setContentDisposition("attachment; filename=\"" + encodedFriendlyName + "\"");
        // 如果需要，也可以设置Content-Type，但OSS通常会根据文件后缀自动设置
        // responseHeaders.setContentType("application/octet-stream"); // 或者根据实际文件类型

        request.setResponseHeaders(responseHeaders);

        String endpoint = aliOssUtil.getEndpoint();
        String accessKeyId = aliOssUtil.getAccessKeyId();
        String accessKeySecret = aliOssUtil.getAccessKeySecret();

        OSS oss = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 生成签名URL
        URL signedUrl = oss.generatePresignedUrl(request);
        return signedUrl.toString();
    }



}
