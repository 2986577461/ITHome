package com.xiaoyan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoyan.pojo.Article;
import com.xiaoyan.vo.ArticleVO;
import com.xiaoyan.vo.MyArticleVO;
import com.xiaoyan.vo.StudentCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;


@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /** 分页查文章，直接带出作者姓名和头像，避免查完再补一次学生信息 */
    List<ArticleVO> selectPage(Integer offset, Integer type, Integer size);

    /** 缓存重建用：每种类型各取最新的 size 条，所以最多返回 类型数 × size 条 */
    List<ArticleVO> selectWindow(Integer size);

    List<Article> selectPageByStudentId(Integer offset, String studentId, Integer size);

    List<MyArticleVO> selectMyPage(Integer offset, String studentId, Integer size);

    List<Article> selectByStudentIds(@Param("studentIds") List<String> studentIds);

    int deleteByStudentIds(@Param("studentIds") List<String> studentIds);

    /** 统计排在指定文章前面的文章数（updated_date_time DESC, id DESC） */
    int countBefore(LocalDateTime updatedDateTime);

    int selectCountByStudentId(String studentId);

    /** 一次查出多个学生的文章数，替代在循环里逐个 count */
    List<StudentCountVO> countByStudentIds(@Param("studentIds") List<String> studentIds);

    void deleteByStudentId(String studentId);
}
