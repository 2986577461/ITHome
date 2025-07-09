package com.xiaoyan.pojo;

import com.xiaoyan.vo.StudentDialogVO;
import lombok.Data;
import lombok.ToString;

import java.util.List;


@Data
@ToString
public class Dialog {

    private String messageType; // 例如 "CHAT", "ONLINE_USER_LIST_UPDATE"
    private String  sender;
    private String  receiver;
    private String content;
    private List<StudentDialogVO> onlineUsers; // 新增字段，用于承载在线用户列表
}
