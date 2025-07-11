package com.xiaoyan.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoyan.pojo.Dialog;
import com.xiaoyan.vo.StudentDialogVO;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@ServerEndpoint("/ws/{sid}/{sname}")
@Slf4j
public class WebSocketServer {

    // 使用复合键管理会话 (sid + sessionId)
    public static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    public static final ObjectMapper objectMapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid, @PathParam("sname") String sname) {
        String sessionKey = sid + "_" + session.getId();

        log.info("新用户连接：ID={}, 名称={}，Session ID={}", sid, sname, session.getId());

        // 存储会话
        sessionMap.put(sessionKey, session);
        session.getUserProperties().put("name", sname);

    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) throws IOException {
        log.info("用户{}发来信息：{}", sid, message);
        Dialog dialog = objectMapper.readValue(message, Dialog.class);
        this.sentToOneClient(dialog.getContent(), dialog.getReceiver());
    }

    @OnClose
    public void onClose(Session session, @PathParam("sid") String sid) {
        String sessionKey = sid + "_" + session.getId();
        sessionMap.remove(sessionKey);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket发生错误，Session ID={}", session.getId(), error);
    }

    private void sentToOneClient(String message, String id) throws IOException {
        Session session1 = sessionMap.get(id);
        if (session1 != null && session1.isOpen()) {
            session1.getBasicRemote().sendText(message);
        }
    }

    private List<StudentDialogVO> getCurrentOnlineUsers() {
        Set<String> uniqueUserIds = new HashSet<>();
        List<StudentDialogVO> onlineUsers = new ArrayList<>();

        for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
            String[] parts = entry.getKey().split("_", 2);
            if (parts.length == 2) {
                try {
                    String  userId = parts[0];

                    // 确保每个用户只添加一次
                    if (uniqueUserIds.add(userId)) {
                        StudentDialogVO vo = new StudentDialogVO();
                        vo.setStudentId(userId);
                        vo.setName((String) entry.getValue().getUserProperties().get("name"));
                        onlineUsers.add(vo);
                    }
                } catch (NumberFormatException e) {
                    log.warn("无效的用户ID格式: {}", parts[0]);
                }
            }
        }
        return onlineUsers;
    }

    public void sendOnlineUserListToAllClients() {
        List<StudentDialogVO> onlineUsers = getCurrentOnlineUsers();
        log.info("当前在线用户列表已更新：{}", onlineUsers.stream().map(StudentDialogVO::getName).collect(Collectors.toList()));

        Dialog onlineListMessage = new Dialog();
        onlineListMessage.setMessageType("ONLINE_USER_LIST_UPDATE");
        onlineListMessage.setOnlineUsers(onlineUsers);

        try {
            String jsonToSend = objectMapper.writeValueAsString(onlineListMessage);

            for (Session session : sessionMap.values()) {
                if (session != null && session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(jsonToSend);
                    } catch (IOException e) {
                        log.error("发送在线用户列表失败: {}", e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error("序列化在线用户列表失败: {}", e.getMessage());
        }
    }
}