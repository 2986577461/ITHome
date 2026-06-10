package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.constant.PositionConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.PasswordDTO;
import com.xiaoyan.interceptor.JwtWhiteList;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.vo.StudentVO;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS;

/**
 * @author yuchao
 */
@Service
@AllArgsConstructor
public class UsersServiceImpl extends ServiceImpl<UserMapper, Student>
        implements UsersService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private JwtProperties jwtProperties;
    private StringRedisTemplate stringRedisTemplate;
    private StudentFileMapper studentFileMapper;
    private CommonService commonService;
    private JwtWhiteList jwtWhiteList;
    private RedisUtil redisUtil;
    private UserMapper userMapper;

    @Override
    public StudentVO getUser(Integer studentId) {
        try {
            Student student = redisUtil.queryHashWithMutex(CACHE_STUDENTS, String.valueOf(studentId),
                    Student.class, id -> this.lambdaQuery().eq(Student::getStudentId, id).one());
            if (student == null) {
                return null;
            }
            StudentVO vo = BeanUtil.toBean(student, StudentVO.class);
            Long avatarId = student.getAvatarId();
            if (avatarId != null) {
                StudentFile avatar = studentFileMapper.selectById(avatarId);
                if (avatar != null) {
                    vo.setAvatar(avatar.getFileUrl());
                }
            }
            return vo;
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public void uploadAvatar(MultipartFile avatar) throws IOException {
        Integer studentId = BaseContext.getCurrentStudentId();
        Student student = userMapper.selectByStudentId(studentId);

        Long avatarId = student.getAvatarId();
        if (avatarId != null) {
            StudentFile oldAvatar = studentFileMapper.selectById(avatarId);
            if (oldAvatar != null) {
                commonService.delete(oldAvatar.getObjectName());
            }
        }
        Long newAvatarId = commonService.upload(avatar);
        student.setAvatarId(newAvatarId);
        redisUtil.save(CACHE_STUDENTS + studentId, student);
        this.lambdaUpdate().set(Student::getAvatarId, newAvatarId).update();

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
        // URL编码并处理空格
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition =
                "attachment; filename=\"" + encodedFileName + "\"; filename*=utf-8''" + encodedFileName;

        headers.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);

        // Content-Length: 告诉浏览器文件大小，有助于下载进度显示
        headers.setContentLength(excelBytes.length);
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }

    @Override
    public Result<StudentVO> login(LoginDTO message) {
        Integer studentId = message.getStudentId();
        String password = message.getPassword();
        Student student = redisUtil.queryHashWithMutex(CACHE_STUDENTS,
                String.valueOf(studentId), Student.class, id -> this.lambdaQuery().
                        eq(Student::getStudentId, Integer.valueOf(id)).one());
        if (student == null) {
           return Result.error(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!ENCODER.matches(password, student.getPassword())) {
           return Result.error(MessageConstant.PASSWORD_ERROR);
        }
        StudentVO vo = BeanUtil.toBean(student, StudentVO.class);

        StudentFile avatar = studentFileMapper.selectById(student.getAvatarId());
        if (avatar != null) {
            vo.setAvatar(avatar.getFileUrl());
        }

        BaseContext.setCurrentStudentId(vo.getStudentId());
        String tokenName;
        if (vo.getPosition().equals(PositionConstant.MASTER)) {
            tokenName = JwtClaimsConstant.ADMIN_ID;
        } else {
            tokenName = JwtClaimsConstant.USER_ID;
        }
        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(tokenName, vo.getStudentId());
        String token = JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                claims);
        vo.setToken(token);
        //添加到token白名单
        jwtWhiteList.addOrUpdateTokenHash(token);
        return Result.success(vo);
    }


    @Override
    public List<StudentVO> getAll() {
        return redisUtil.getAllWithHashCache(CACHE_STUDENTS,
                this::count, this.query()::list, Student.class, StudentVO.class);
    }

    @Override
    public void removeStudents(List<Long> ids) {
        userMapper.deleteByIds(ids);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS,
                (Object[]) ids.stream().map(String::valueOf).toArray(String[]::new));

    }

    @Override
    public void update(Student student) {
        String password = student.getPassword();
        if (password != null) {
            student.setPassword(ENCODER.encode(password));
        }
        userMapper.updateById(student);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(student.getStudentId()));
    }

    @Override
    public void updatePassword(PasswordDTO passwordDTO, Integer studentId) {
        this.lambdaUpdate().set(Student::getPassword, ENCODER.encode(passwordDTO.getPassword())).
                eq(Student::getStudentId, studentId).update();

        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(studentId));
    }


}