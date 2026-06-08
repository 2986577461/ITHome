package com.xiaoyan.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "xiaoyan")
public class AdmitUrlProperties {
    private List<String> admitUrls;
}