package com.xiaoyan.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@AllArgsConstructor
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private OSS ossClient;

    /**
     * 文件删除
     *
     * @param objectName 要删除的文件在OSS中的完整路径/名称 (例如: "images/photo.jpg")
     */
    public void delete(String objectName) {
        try {
            // 删除文件。
            ossClient.deleteObject(bucketName, objectName);
            log.info("文件 {} 从 OSS 删除成功。", objectName);
        } catch (OSSException oe) {
            log.error("Caught an OSSException during delete operation. "
                            + "Error Message: {}, Error Code: {}, Request ID: {}, Host ID: {}",
                    oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            // 抛出运行时异常，让上层处理，或者根据需要返回false
            throw new RuntimeException("文件删除失败: " + oe.getErrorMessage(), oe);
            // return false; // 如果你想返回false而不是抛出异常
        } catch (ClientException ce) {
            log.error("Caught a ClientException during delete operation. "
                    + "Error Message: {}", ce.getMessage());
            // 抛出运行时异常
            throw new RuntimeException("文件删除失败: 客户端网络或内部问题", ce);
            // return false; // 如果你想返回false而不是抛出异常

        }
    }

    /** 批量删除文件 */
    public void deleteObjects(List<String> objectNames) {
        try {
            ossClient.deleteObjects(
                    new DeleteObjectsRequest(bucketName).withKeys(objectNames));
            log.info("{} 个文件从 OSS 批量删除成功", objectNames.size());
        } catch (OSSException oe) {
            log.error("OSSException: {}, Error Code: {}", oe.getErrorMessage(), oe.getErrorCode());
            throw new RuntimeException("批量删除文件失败: " + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            log.error("ClientException: {}", ce.getMessage());
            throw new RuntimeException("批量删除文件失败: 客户端网络或内部问题", ce);
        }
    }

    /**
     * 探测 OSS 是否可用。
     *
     * <p>给补传任务用：一轮要补传 N 个文件，先探一次就够了，比让 N 次上传各自失败一遍省得多。</p>
     *
     * <p>不抛异常——「探测失败」和「OSS 不可用」是同一件事，调用方不该再写 try-catch。
     * SDK 的 {@code doesBucketExist} 本身对网络异常就是吞掉返回 false，正好是这里要的语义。</p>
     *
     * @return true 表示可以开始补传
     */
    public boolean isAvailable() {
        try {
            return ossClient.doesBucketExist(bucketName);
        } catch (Exception e) {
            log.warn("OSS 探测失败: {}", e.getMessage());
            return false;
        }
    }

    public String upload(byte[] bytes, String objectName) {
        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            // 不能吞掉异常：否则文件根本没传上去，这里还是会拼出一个「看起来成功」的 URL 返回给前端
            log.error("上传文件失败. Error Message: {}, Error Code: {}, Request ID: {}, Host ID: {}",
                    oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            throw new RuntimeException("上传文件失败: " + oe.getErrorMessage(), oe);
        } catch (ClientException ce) {
            log.error("上传文件失败. Error Message: {}", ce.getMessage());
            throw new RuntimeException("上传文件失败: 客户端网络或内部问题", ce);
        }


        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder);

        return stringBuilder.toString();
    }

    public String getDownloadUrl(String objectName, String friendlyName, Long expirationMillis) {
        // 创建GeneratePresignedUrlRequest对象
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectName);

        // 设置签名URL的有效时间
        Date expiration = new Date(System.currentTimeMillis() + expirationMillis);
        request.setExpiration(expiration);

        // 设置Content-Disposition，指定下载的文件名
        ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();

        try {
            // 方案一：使用 RFC 5987 规范，直接 UTF-8 URL 编码
            // 这种方式在现代浏览器中兼容性更好
            String encodedFriendlyNameUtf8 = URLEncoder.encode(friendlyName, StandardCharsets.UTF_8);
            // RFC 5987 格式: filename*=<charset>'<language>'<encoded-text>
            // 例如: filename*=UTF-8''%E4%BD%A0%E5%A5%BD.txt (你好.txt)
            responseHeaders.setContentDisposition("attachment; filename*=UTF-8''" + encodedFriendlyNameUtf8);

            // 如果上述方案仍然有问题，可以尝试同时提供两种 filename (filename 和 filename*)
            // 有些浏览器会优先使用 filename*，如果不支持则退回 filename
            // 这种情况下，你需要同时设置两种，但需要确保它们的编码都是正确的
            // String encodedFriendlyNameIso = new String(friendlyName.getBytes(StandardCharsets.UTF_8),
            // StandardCharsets.ISO_8859_1);
            // responseHeaders.setContentDisposition("attachment; filename=\"" + encodedFriendlyNameIso + "\";
            // filename*=UTF-8''" + encodedFriendlyNameUtf8);

        } catch (Exception e) {
            // 处理编码异常，虽然 URLEncoder 很少抛出，但还是加上
            log.error("文件名编码失败: {}", friendlyName, e);
            responseHeaders.setContentDisposition("attachment; filename=\"download\""); // 回退到默认文件名
        }

        request.setResponseHeaders(responseHeaders);

        // 生成签名URL
        URL signedUrl = ossClient.generatePresignedUrl(request);
        return signedUrl.toString();
    }

}