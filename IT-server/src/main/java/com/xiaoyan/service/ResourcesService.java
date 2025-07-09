package com.xiaoyan.service;


import com.xiaoyan.pojo.Resources;
import com.xiaoyan.vo.ResourcesVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ResourcesService  {

    Long getCount();

    String generatePresignedDownloadUrl(String objectName, String friendlyFileName, long expirationMillis);

    List<ResourcesVO> getList();

    void saveResource(Resources resources);

    void deleteById(String id);
}
