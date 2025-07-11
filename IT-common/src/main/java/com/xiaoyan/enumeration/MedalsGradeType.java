package com.xiaoyan.enumeration;

public enum MedalsGradeType {
    COLLEGE(1, "院级"),
    SCHOOL(2, "校级"),
    CITY(3, "市级"),
    PROVINCE(4,"省级"),
    COUNTRY(5,"全国级");

    private final int code;
    private final String description;

    MedalsGradeType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MedalsGradeType fromCode(int code) {
        for (MedalsGradeType type : MedalsGradeType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ArticleType code: " + code);
    }
}
