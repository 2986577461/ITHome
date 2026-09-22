package com.xiaoyan.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OSS 删除失败的欠账，见 {@code pending_oss_delete} 表。
 *
 * <p>没有对应的实体类、也不继承 {@link com.baomidou.mybatisplus.core.mapper.BaseMapper}：
 * 这张表只有 object_name 一个业务字段，查询也只需要它，建个 pojo 除了让 MyBatis-Plus
 * 的自动填充插一脚之外没别的作用——而 insert 恰恰不能用它的默认语义，
 * 重复记账必须是被忽略而不是抛主键冲突。</p>
 *
 * <p>三个方法都靠 object_name 主键做到幂等：重复记账被 ignore，重复删除无副作用，
 * 所以调用方不需要关心自己是不是第一个来的。</p>
 */
@Mapper
public interface PendingOssDeleteMapper {

    /**
     * 记账。用 {@code insert ignore}：同一个对象被记两次是可能的（OSS 上一轮就没删掉，
     * 这一轮又有人删它），这时候要静默跳过而不是把调用方的事务炸掉。
     */
    @Insert("<script>insert ignore into pending_oss_delete (object_name, create_date_time) values "
            + "<foreach collection='objectNames' item='objectName' separator=','>"
            + "(#{objectName}, #{createDateTime})"
            + "</foreach></script>")
    int insertIgnore(@Param("objectNames") List<String> objectNames,
                     @Param("createDateTime") LocalDateTime createDateTime);

    /** 待重试的清单，攒得久的排在前面。取一批就够，剩下的下一轮再说 */
    @Select("select object_name from pending_oss_delete order by create_date_time limit #{limit}")
    List<String> selectPending(@Param("limit") int limit);

    /** 在 OSS 上删成功之后才调用，把欠账平掉 */
    @Delete("<script>delete from pending_oss_delete where object_name in "
            + "<foreach collection='objectNames' item='objectName' open='(' separator=',' close=')'>"
            + "#{objectName}</foreach></script>")
    int deleteByObjectNames(@Param("objectNames") List<String> objectNames);
}
