package com.xiaoyan.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.AiDialogSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Mapper
public interface AiDialogSessionMapper extends BaseMapper<AiDialogSession> {

    @Select("select * from ai_dialog_session where student_id=#{studentId} and deleted=0;")
    ArrayList<AiDialogSession> selectByStudentId(Integer studentId);

    @Update("update ai_dialog_session set last_active_date_time=#{now} where id=#{id} and deleted=0")
    void updateLastActiveDateTime(Integer id, LocalDateTime now);

    @Update("update ai_dialog_session set title=#{title} where id=#{id} and deleted=0")
    void updateTitleByGroupId(Integer id, String title);
}
