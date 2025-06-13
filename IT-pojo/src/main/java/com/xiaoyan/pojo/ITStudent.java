package com.xiaoyan.pojo;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.StringJoiner;

/**
 * @author yuchao
 */

@Data
@TableName("it_student")
public class ITStudent {

    @TableId
    private String studentId;

    private String name;

    private String sex;

    private String major;

    private String password;

    @TableField("class")
    private String claxx;

    private String academy;

    private String position;

    private Integer articleCount;

    private Integer resourceCount;

    public ITStudent() {
    }

    public ITStudent(Newcomer newcomer){
      this.studentId =  newcomer.getStudentId();
      this.name=newcomer.getName();
      this.sex=newcomer.getSex();
      this.major=newcomer.getMajor();
      this.claxx=newcomer.getClaxx();
      this.academy=newcomer.getAcademy();
      this.password="123456";
      this.position="学员";
      this.articleCount=0;
      this.resourceCount=0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ITStudent.class.getSimpleName() + "[", "]")
                .add("studentId='" + studentId + "'")
                .add("name='" + name + "'")
                .add("sex='" + sex + "'")
                .add("major='" + major + "'")
                .add("password='" + password + "'")
                .add("claxx='" + claxx + "'")
                .add("academy='" + academy + "'")
                .add("position='" + position + "'")
                .add("articleCount=" + articleCount)
                .add("resourceCount=" + resourceCount)
                .toString();
    }
}
