package com.xiaoyan.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.xiaoyan.pojo.StudentFile;

import java.util.List;

@Mapper
public interface StudentFileMapper extends BaseMapper<StudentFile> {

    void deleteByObjectNames(@Param("objectNames") List<String> objectNames);

    @Select("select * from student_file where object_name=#{objectName};")
    StudentFile selectbyObjectName(String objectName);

    /**
     * 补传任务的待办清单：OSS 当时不可用、文件还落在本机的那些。
     *
     * <p>注解查询绕过了 MyBatis-Plus 的逻辑删除注入，{@code deleted} 条件得自己写。
     * 这里没写是因为这个表的删除走的是 {@code deleteByObjectNames} 的物理 DELETE，
     * {@code deleted} 列实际没在用。</p>
     */
    @Select("select * from student_file where storage_type = 'LOCAL'")
    List<StudentFile> selectPendingSync();
}