package com.xiaoyan.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;

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
     * @return true表示删除成功，false表示删除失败
     */
    public boolean delete(String objectName) {
        try {
            // 删除文件。
            ossClient.deleteObject(bucketName, objectName);
            log.info("文件 {} 从 OSS 删除成功。", objectName);
            return true;
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

    public String upload(byte[] bytes, String objectName) {

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
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
