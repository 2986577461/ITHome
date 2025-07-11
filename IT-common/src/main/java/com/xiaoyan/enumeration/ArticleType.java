package com.xiaoyan.enumeration;

public enum ArticleType {
    NOTICE(1, "通知"),
    ANNOUNCEMENT(2, "公告"),
    NEWS(3, "新闻");

    private final int code;
    private final String description;

    ArticleType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ArticleType fromCode(int code) {
        for (ArticleType type : ArticleType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ArticleType code: " + code);
    }
}