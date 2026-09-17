package com.xiaoyan.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Aspect
@Component
@Slf4j
@AllArgsConstructor
public class ControllerLogAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "token", "authorization", "secret", "accesskey", "accesskeyid",
            "accesskeysecret", "apikey", "api_key", "jwt"
    );

    private static final int MAX_STRING_LENGTH = 1_000;

    private final ObjectMapper objectMapper;

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object logControllerRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();

        String sourceAddress = request == null ? "unknown" : resolveClientIp(request);
        String deviceName = request == null ? "unknown" : resolveDeviceName(request.getHeader("User-Agent"));
        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();

        log.info("请求来源地址={}, 设备名={}, 方法名={}, 实际参数={}",
                sourceAddress, deviceName, methodName, serializeArguments(joinPoint.getArgs()));

        return joinPoint.proceed();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }

    private String resolveDeviceName(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }

        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("ipad")) {
            return "iPad";
        }
        if (normalized.contains("iphone")) {
            return "iPhone";
        }
        if (normalized.contains("android")) {
            return "Android";
        }
        if (normalized.contains("windows")) {
            return "Windows PC";
        }
        if (normalized.contains("macintosh")) {
            return "macOS";
        }
        if (normalized.contains("linux")) {
            return "Linux";
        }
        return "unknown";
    }

    private String serializeArguments(Object[] arguments) {
        List<JsonNode> safeArguments = new ArrayList<>();
        for (Object argument : arguments) {
            if (argument == null || argument instanceof HttpServletRequest
                    || argument instanceof HttpServletResponse
                    || argument instanceof InputStream) {
                continue;
            }
            safeArguments.add(toSafeJson(argument));
        }

        try {
            return objectMapper.writeValueAsString(safeArguments);
        } catch (Exception exception) {
            return "[unserializable arguments]";
        }
    }

    private JsonNode toSafeJson(Object argument) {
        if (argument instanceof MultipartFile file) {
            ObjectNode fileNode = objectMapper.createObjectNode();
            fileNode.put("originalFilename", file.getOriginalFilename());
            fileNode.put("size", file.getSize());
            fileNode.put("contentType", file.getContentType());
            return fileNode;
        }

        try {
            JsonNode node = objectMapper.valueToTree(argument);
            maskSensitiveFields(node);
            truncateLongStrings(node);
            return node;
        } catch (IllegalArgumentException exception) {
            return TextNode.valueOf(argument.getClass().getSimpleName());
        }
    }

    private void maskSensitiveFields(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode field = objectNode.get(fieldName);
                if (SENSITIVE_FIELDS.contains(fieldName.toLowerCase(Locale.ROOT))) {
                    objectNode.put(fieldName, "******");
                } else {
                    maskSensitiveFields(field);
                }
            });
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::maskSensitiveFields);
        }
    }

    private void truncateLongStrings(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode field = objectNode.get(fieldName);
                if (field.isTextual() && field.textValue().length() > MAX_STRING_LENGTH) {
                    objectNode.put(fieldName, field.textValue().substring(0, MAX_STRING_LENGTH) + "...truncated");
                } else {
                    truncateLongStrings(field);
                }
            });
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::truncateLongStrings);
        }
    }
}
