package com.xiaoyan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaoyan.result.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.vo.StudentVO;

import java.io.IOException;
import java.util.List;


public interface UsersService extends IService<Student> {

    StudentVO getUser(Integer studentId);

    /**
     * 校验当前登录用户是否有权限操作归属于 ownerStudentId 的资源：仅作者本人或管理员可以。
     *
     * @param ownerStudentId 资源归属的学生 id
     */
    void checkOwnerOrAdmin(Integer ownerStudentId);

    Result<StudentVO> login(LoginDTO message);

    void removeStudents(List<Integer > studentIds);

    List<StudentVO> getAll();

    void update(Student student);

    void uploadAvatar(MultipartFile avatar) throws IOException;

    ResponseEntity<byte[]> downloadExcel() throws IOException;

}