package com.xiaoyan.constant;

public class RedisConstant {
    public static final String CACHE_RESOURCES_ALL="cache:resources:all";
    public static final String CACHE_STUDENTS="cache:students";
    public static final String CACHE_STUDENTS_ALL="cache:students:all";
    public static final String CACHE_NEWCOMERS="cache:newcomers";
    public static final String CACHE_ARTICLES="cache:articles";
    public static final String RANKING_ARTICLES="ranking:articles";

    /** 文章缓存重建/写入的分布式锁，保证多实例下同一时刻只有一个实例在改缓存 */
    public static final String LOCK_ARTICLE_CACHE="lock:articles:cache";

    /** 文章缓存构建完成标记：只有它不存在时才触发重建 */
    public static final String CACHE_ARTICLES_READY=RANKING_ARTICLES+":ready";
}
