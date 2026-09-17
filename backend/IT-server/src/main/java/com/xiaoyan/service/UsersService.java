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

    StudentVO getUser(String studentId);

    /**
     * 校验当前登录用户是否有权限操作归属于 ownerStudentId 的资源：仅作者本人或管理员可以。
     *
     * @param ownerStudentId 资源归属的学生 id
     */
    void checkOwnerOrAdmin(String ownerStudentId);

    Result<StudentVO> login(LoginDTO message);

    void removeStudents(List<String> studentIds);

    /**
     * 注销当前登录账号，连同他发布的文章、上传的资料和文件一起删除。
     *
     * <p>无需权限校验（删的是自己），但最后一位管理员不能注销，
     * 否则协会后台会没人能审批新成员。</p>
     */
    void removeSelf();

    List<StudentVO> getAll();

    void update(Student student);

    void uploadAvatar(MultipartFile avatar) throws IOException;

    ResponseEntity<byte[]> downloadExcel() throws IOException;

}