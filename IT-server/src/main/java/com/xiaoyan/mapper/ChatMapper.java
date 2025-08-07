package com.xiaoyan.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.ChatDialog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMapper extends BaseMapper<ChatDialog> {

    @Select("select * from chat_dialog where (sender_id=#{studentId} or receiver_id=#{studentId}) and deleted=0;")
    List<ChatDialog> selectHistoryMessageByStudentId(Integer studentId);
}
