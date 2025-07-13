package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.AiDialog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiDialogMapper extends BaseMapper<AiDialog> {

    @Select("select * from ai_dialog where session_id=#{sessionId};")
    List<AiDialog> selectBySessionId(Integer sessionId);

    @Delete("delete from ai_dialog where session_id=#{sessionId}")
    void deleteBySessionId(Integer sessionId);

}
