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

    Result<StudentVO> login(LoginDTO message);

    void removeStudents(List<Integer > studentIds);

    List<StudentVO> getAll();

    void update(Student student);

    void uploadAvatar(MultipartFile avatar) throws IOException;

    ResponseEntity<byte[]> downloadExcel() throws IOException;

}