package com.xiaoyan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.UpdateStudent;
import com.xiaoyan.pojo.ITStudent;

import com.xiaoyan.vo.ITStudentVO;
import com.xiaoyan.vo.StudentGovernVO;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;


public interface UsersService extends IService<ITStudent> {

    ITStudentVO getUser();

    ITStudentVO login(LoginDTO message);

    void removeStudents(@NotNull ArrayList<String > students);

    List<ITStudent> selectAllMember() ;

    void updateStudent( @NotNull String id,@Validated UpdateStudent student) ;

    void updatePassword(@NotNull String id,@NotNull String pswd) ;

    List<StudentGovernVO> getAll();
}
