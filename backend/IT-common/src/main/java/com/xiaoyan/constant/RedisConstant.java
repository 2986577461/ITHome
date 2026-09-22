package com.xiaoyan.constant;

public class RedisConstant {
    public static final String CACHE_RESOURCES_ALL="cache:resources:all";
    public static final String CACHE_STUDENTS="cache:students";
    public static final String CACHE_NEWCOMERS="cache:newcomers";
    /**
     * 文章分页缓存：一个 Hash 装下全部榜单的全部分页。
     *
     * <p>field = {@code type:size:page}，value = 那一页的 ArticleVO JSON 数组。
     * 把 size 和 page 放进 field 而不是拼进 key，是为了让「清空整组缓存」始终是一次 DEL。</p>
     */
    public static final String CACHE_ARTICLE_PAGES="cache:articles:page";
}