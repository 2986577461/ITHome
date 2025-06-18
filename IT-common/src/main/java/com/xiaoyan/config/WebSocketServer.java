package com.xiaoyan.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.pojo.Dialog;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    // 存放会话对象
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    public static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        sessionMap.put(sid, session);
    }

    /**
     * 收到客户端消息后调用的方法
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) throws IOException {
        log.info("用户{}发来信息：{}", sid, message);
        Dialog dialog = objectMapper.readValue(message, Dialog.class);
        sendToOneClient(dialog);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        sessionMap.remove(sid);
    }

    public void sendToOneClient(Dialog dialog) throws IOException {
        Session session = sessionMap.get(dialog.getReceiver());
        //服务器向客户端发送消息
        if (session != null)
            session.getBasicRemote().sendText(objectMapper.writeValueAsString(dialog));
    }

}