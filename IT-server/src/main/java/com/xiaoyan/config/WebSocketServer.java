package com.xiaoyan.config;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.dto.ChatDialogDTO;
import com.xiaoyan.pojo.ChatDialog;
import com.xiaoyan.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yuchao
 */
@Component
@ServerEndpoint("/ws/{sid}/{sname}")
@Slf4j
public class WebSocketServer{

    public static final Map<Integer, Session> SESSION_MAP = new ConcurrentHashMap<>();
    public static ObjectMapper objectMapper;

    // 将 chatService 声明为静态变量
    private static ChatService chatService;

    @Autowired // @Autowired 注解在静态 set 方法上
    public void setChatService(ChatService chatService) {
        WebSocketServer.chatService = chatService;
    }
    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        WebSocketServer.objectMapper = objectMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") Integer sid, @PathParam("sname") String sname) throws IOException {
        log.info("新用户连接：ID={}, 名称={}，Session ID={}", sid, sname, session.getId());
        SESSION_MAP.put(sid, session);
        List<ChatDialog> list= chatService.getAllHistoryMessage(sid);
        loadChatRecordHistory(sid,list);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") Integer sid) throws IOException {
        log.info("用户{}发来信息：{}", sid, message);
        ChatDialogDTO dialog = objectMapper.readValue(message, ChatDialogDTO.class);
        this.sentToOneClient(dialog);
    }

    @OnClose
    public void onClose(@PathParam("sid") Integer sid) {
        SESSION_MAP.remove(sid);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误，Session ID={}", session.getId(), error);
    }

    private void sentToOneClient(@Valid ChatDialogDTO dialogDTO) throws IOException {
        ChatDialog chatDialog = new ChatDialog();
        BeanUtils.copyProperties(dialogDTO, chatDialog);
        // 设置消息发送时间
        chatDialog.setCreateDateTime(LocalDateTime.now());

        chatService.saveMessage(chatDialog);

        Session receiverSession = SESSION_MAP.get(dialogDTO.getReceiverId());

        // 4. 如果接收者在线，发送完整的 ChatDialog 对象（JSON 格式）
        if (receiverSession != null && receiverSession.isOpen()) {
            String jsonMessage = JSONUtil.toJsonStr(chatDialog);
            receiverSession.getBasicRemote().sendText(jsonMessage);

        }
    }
    private void loadChatRecordHistory(@NotNull Integer studentId, List<ChatDialog> list) throws IOException {
        Session session = SESSION_MAP.get(studentId);
        if (session != null && session.isOpen()) {
            String jsonList = JSONUtil.toJsonStr(list);
            session.getBasicRemote().sendText(jsonList);
        }
    }
}