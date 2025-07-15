package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.AiDialog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AiDialogMapper extends BaseMapper<AiDialog> {

    @Select("select * from ai_dialog where session_id=#{sessionId} and deleted=0;")
    List<AiDialog> selectBySessionId(Integer sessionId);

    @Update("update ai_dialog set deleted=1 where session_id=#{sessionId} and deleted=0")
    void deleteBySessionId(Integer sessionId);
}
