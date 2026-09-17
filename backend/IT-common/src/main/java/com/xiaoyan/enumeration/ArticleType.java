package com.xiaoyan.enumeration;

/**
 * 文章分类。
 *
 * <p>code 显式声明而不依赖 ordinal()：ordinal 会随枚举顺序变化，
 * 而它同时被写进数据库的 type 字段和 Redis 榜单 key，顺序一变数据就对不上。</p>
 */
public enum ArticleType {
    ALL(0),
    C_LANGUAGE(1),
    HTML(2),
    CSS(3),
    JAVA_SCRIPT(4),
    JAVA(5),
    MYSQL(6);

    private final int code;

    ArticleType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
