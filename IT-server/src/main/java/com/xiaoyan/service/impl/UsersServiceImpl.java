package com.xiaoyan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.PasswordDTO;
import com.xiaoyan.exception.AccountNotFoundException;
import com.xiaoyan.exception.PasswordErrorException;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;

import com.xiaoyan.service.UsersService;
import com.xiaoyan.vo.StudentVO;
import jakarta.annotation.Resource;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Validated
public class UsersServiceImpl extends ServiceImpl<UserMapper, Student>
        implements UsersService {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Resource
    private UserMapper userMapper;

    @Override
    public StudentVO getUser(Integer studentId) {
        Student student = userMapper.selectByStudentId(studentId);
        StudentVO studentVO = new StudentVO();
        BeanUtils.copyProperties(student, studentVO);

        return studentVO;
    }

    @Override
    public StudentVO login(LoginDTO message) {
        Integer studentId = message.getStudentId();
        String password = message.getPassword();
        Student student = userMapper.selectByStudentId(studentId);
        if (student == null)
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);

        if (!encoder.matches(password, student.getPassword()))
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);

        StudentVO studentVO = new StudentVO();
        BeanUtils.copyProperties(student, studentVO);

        BaseContext.setCurrentStudentId(studentVO.getStudentId());

        return studentVO;
    }

    @Override
    @CacheEvict(cacheNames = {"userList"},allEntries = true)
    public void removeStudents(List<Integer> ids) {
        userMapper.deleteByIds(ids);
    }


    @Override
    @Cacheable(value = "userList",key = "'userList'")
    public List<StudentVO> getAll() {
        List<Student> students = userMapper.selectList(null);
        List<StudentVO> studentVOS = new ArrayList<>();
        for (Student student : students) {
            StudentVO studentVO = new StudentVO();
            BeanUtils.copyProperties(student, studentVO);
            studentVOS.add(studentVO);
        }
        return studentVOS;
    }


    @Override
    @CacheEvict(cacheNames = {"userList","articlesList"},allEntries = true)
    public void update(Student student) {
        String password = student.getPassword();
        if(password!=null)
            student.setPassword(encoder.encode(password));
        userMapper.updateById(student);
    }

    @Override
    public void updatePassword(PasswordDTO passwordDTO, Integer studentId) {
        userMapper.updatePasswordByStudentId(
                studentId, encoder.encode(passwordDTO.getPassword()));
    }

    @Override
    public ResponseEntity<byte[]> downloadExcel() throws IOException {
        List<StudentVO> all = this.getAll();
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        XSSFSheet sheet = xssfWorkbook.createSheet();
        for (int i = 0; i <= 6; i++) {
            sheet.setColumnWidth(i, 4000);
        }

        XSSFRow row = sheet.createRow(0);
        //合并单元格
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        row.createCell(0).setCellValue("IT之家协会花名册");

        XSSFRow row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("学号");
        row1.createCell(1).setCellValue("姓名");
        row1.createCell(2).setCellValue("性别");
        row1.createCell(3).setCellValue("专业");
        row1.createCell(4).setCellValue("班级");
        row1.createCell(5).setCellValue("学院");
        row1.createCell(6).setCellValue("职务");

        for (int i = 2, s = 0; s < all.size(); i++, s++) {
            XSSFRow row2 = sheet.createRow(i);
            row2.createCell(0).setCellValue(all.get(s).getStudentId());
            row2.createCell(1).setCellValue(all.get(s).getName());
            row2.createCell(2).setCellValue(all.get(s).getSex());
            row2.createCell(3).setCellValue(all.get(s).getMajor());
            row2.createCell(4).setCellValue(all.get(s).getClassName());
            row2.createCell(5).setCellValue(all.get(s).getAcademy());
            row2.createCell(6).setCellValue(all.get(s).getPosition());
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        xssfWorkbook.write(bos);
        xssfWorkbook.close();

        byte[] excelBytes = bos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        // Content-Type: 告诉浏览器响应的内容类型是 Excel 文件
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml" +
                ".sheet"));

        // Content-Disposition: 告诉浏览器这是一个附件，并指定下载的文件名
        String fileName = "IT之家协会花名册.xlsx";
        // filename* 参数用于 UTF-8 编码的文件名，优先被现代浏览器识别
        // filename 参数用于兼容旧浏览器，通常使用 ISO-8859-1 编码（或直接使用原始字符串，由浏览器自行处理）
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"); // URL编码并处理空格
        String contentDisposition =
                "attachment; filename=\"" + encodedFileName + "\"; filename*=utf-8''" + encodedFileName;

        headers.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);

        // Content-Length: 告诉浏览器文件大小，有助于下载进度显示
        headers.setContentLength(excelBytes.length);
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }

}
