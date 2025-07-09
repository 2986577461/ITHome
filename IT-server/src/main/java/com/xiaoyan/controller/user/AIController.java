package com.xiaoyan.controller.user;


import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.service.AIService;
import com.xiaoyan.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/user/ai-dialog")
@AllArgsConstructor
@Tag(name = "AI管理")
@Slf4j
public class AIController {

    private AIService aiService;

    @Resource
    private JwtProperties jwtProperties;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAiResponse(String message, String token) {
        temporaryJwtParse(token);

        return aiService.streamChatCompletion(BaseContext.getCurrentStudentId(), message)
                .map(chunk -> "data: " + chunk + "\n");
    }

    private void temporaryJwtParse(String token) {
        Claims claims = JwtUtil.parseJWT(jwtProperties.getSecretKey(), token);
        Object object = claims.get(JwtClaimsConstant.USER_ID);
        Object object1 = claims.get(JwtClaimsConstant.ADMIN_ID);
        Integer studentId;
        if (object != null)
            studentId = Integer.valueOf(object.toString());
        else
            studentId = Integer.valueOf(object1.toString());

        BaseContext.setCurrentStudentId(studentId);
    }


}
