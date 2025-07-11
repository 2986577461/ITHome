package com.xiaoyan.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;


    /**
     * 文件删除
     *
     * @param objectName 要删除的文件在OSS中的完整路径/名称 (例如: "images/photo.jpg")
     * @return true表示删除成功，false表示删除失败
     */
    public boolean delete(String objectName) {
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

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
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

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
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
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

}
