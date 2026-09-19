package com.xiaoyan.controller.user;

import com.xiaoyan.result.Result;
import com.xiaoyan.service.CommonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;


@Slf4j
@RestController
@RequestMapping("user/common")
@AllArgsConstructor
@Tag(name = "公共组件")
public class CommonController {

    /** 预签名 URL 的有效期 */
    private static final long EXPIRATION_MILLIS = 60 * 1000;

    private CommonService commonService;

    /**
     * 获取文件下载地址。
     *
     * <p>文件在 OSS 上就给预签名地址；当时没传上 OSS 还在本机，就给本地路径
     * {@code /user/common/local/{objectName}}。两者都是能直接塞进 {@code <a href>}
     * 的 URL，前端不用区分。</p>
     */
    @GetMapping("/url")
    @Operation(summary = "获取文件下载地址")
    public Result<String> getDownloadUrl(String objectName) {
        return Result.success(commonService.generateDownloadUrl(objectName, EXPIRATION_MILLIS));
    }

    /**
     * 降级文件的访问入口，附件或内联由文件类型决定。<b>免登录</b>
     * （在 xiaoyan.admit-urls 里）。
     *
     * <p>为什么必须免登录：{@code <img src>} 发不出 Authorization 头。OSS 那边的图本来
     * 就是「拿到 object_name 就能看」的公开资源（上传返回的是不带签名的裸 URL），
     * 所以安全模型没变差。</p>
     *
     * <p>本地副本被补传清掉之后会 302 到 {@code file_url} 里回填好的 OSS 地址。文章正文
     * 里烧死的那个本地 URL 就靠这条永远能跟过去，不会因为补传完就坏掉。</p>
     *
     * <p>刻意不用项目统一的「HTTP 200 + body code」：响应体是二进制流，塞不进 Result，
     * 二进制接口用 HTTP 状态码是正常做法。</p>
     */
    @GetMapping("/local/{objectName}")
    @Operation(summary = "访问尚未补传到 OSS 的本地文件")
    public ResponseEntity<Resource> accessLocal(@PathVariable("objectName") String objectName,
                                                @RequestParam(defaultValue = "false") boolean download) {
        CommonService.LocalAccess local = commonService.accessLocalFile(objectName);
        if (local == null) {
            return ResponseEntity.notFound().build();
        }
        if (local.redirectTo() != null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(local.redirectTo()))
                    .build();
        }
        return fileResponse(local, !download && canInline(local.contentType()));
    }

    private ResponseEntity<Resource> fileResponse(CommonService.LocalAccess local, boolean inline) {
        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(local.originalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                // 文件类型是上传时浏览器报的，不可信，别让浏览器自己去猜
                .header("X-Content-Type-Options", "nosniff")
                .contentType(parseMediaType(local.contentType()))
                .body(local.resource());
    }

    /**
     * 能不能内联返回。
     *
     * <p>这个接口是免登录的，而 Content-Type 由上传者控制：一个 {@code image/svg+xml}
     * 内联返回就等于同源的存储型 XSS（SVG 里能跑脚本），{@code text/html} 同理。
     * 所以只对确定安全的位图内联，其余一律退回附件。</p>
     */
    private boolean canInline(String contentType) {
        if (contentType == null) {
            return false;
        }
        String type = contentType.toLowerCase(Locale.ROOT);
        return type.startsWith("image/") && !type.contains("svg");
    }

    private MediaType parseMediaType(String contentType) {
        if (contentType == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            log.warn("无法解析的 Content-Type: {}", contentType);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
