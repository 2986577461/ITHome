package com.xiaoyan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.dto.StudentAccount;
import com.xiaoyan.dto.UpdateStudent;
import com.xiaoyan.pojo.ITStudent;

import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;


@Service
public interface UsersService extends IService<ITStudent> {

    ITStudent getUser(@NotNull String id);

    boolean login(StudentAccount message);

    boolean removeStudents(@NotNull ArrayList<String > students);

    ArrayList<ITStudent> selectAllMember() ;

    boolean updateStudent( @NotNull String id,@Validated UpdateStudent student) ;

    boolean updatePassword(@NotNull String id,@NotNull String pswd) ;

}
