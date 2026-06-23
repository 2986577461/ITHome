package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.interceptor.JwtWhiteList;
import com.xiaoyan.mapper.ArticleMapper;
import com.xiaoyan.mapper.ResourcesMapper;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Article;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS;
import static com.xiaoyan.service.impl.ArticlesServiceImpl.IMAGE_PATTERN;

/**
 * @author yuchao
 */
@Service
@AllArgsConstructor
public class UsersServiceImpl extends ServiceImpl<UserMapper, Student>
        implements UsersService {

    private final ArticleMapper articleMapper;
    private final ResourcesMapper resourcesMapper;
    private JwtProperties jwtProperties;
    private StringRedisTemplate stringRedisTemplate;
    private StudentFileMapper studentFileMapper;
    private CommonService commonService;
    private JwtWhiteList jwtWhiteList;
    private RedisUtil redisUtil;
    private UserMapper userMapper;

    @Override
    public StudentVO getUser(Integer studentId) {
        return redisUtil.queryHashWithMutex(CACHE_STUDENTS, String.valueOf(studentId),
                StudentVO.class, id -> this.queryStudentFromDB(studentId));
    }

    public StudentVO queryStudentFromDB(Integer studentId) {
        Student student = userMapper.selectByStudentId(studentId);
        if (student == null) {
            return null;
        }
        StudentVO vo = BeanUtil.toBean(student, StudentVO.class);
        Long avatarId = student.getAvatarId();
        if (avatarId != null) {
            StudentFile avatar = studentFileMapper.selectById(avatarId);
            if (avatar != null) {
                vo.setAvatar(avatar.getFileUrl());
                vo.setArticleCount(articleMapper.selectCountByStudentId(studentId));
                vo.setResourceCount(resourcesMapper.selectCountByStudentId(studentId));
            }
        }
        return vo;
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
        Long newAvatarId = commonService.upload(avatar).getId();
        student.setAvatarId(newAvatarId);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, studentId);
        this.lambdaUpdate().set(Student::getAvatarId, newAvatarId).update();

    }

    @Override
    public ResponseEntity<byte[]> downloadExcel() throws IOException {
        List<Student> all = userMapper.selectThisYearsStudents();

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
            if (all.get(s).getName().equals("AI协会助手")) {
                s++;
            }
            XSSFRow row2 = sheet.createRow(i);
            row2.createCell(0).setCellValue(all.get(s).getStudentId());
            row2.createCell(1).setCellValue(all.get(s).getName());
            row2.createCell(2).setCellValue(all.get(s).getSex());
            row2.createCell(3).setCellValue(all.get(s).getMajor());
            row2.createCell(4).setCellValue(all.get(s).getClassName());
            row2.createCell(5).setCellValue(all.get(s).getAcademy());

            String position = all.get(s).getPosition();
            row2.createCell(6).setCellValue("admin".equals(position) ? "会长" : "学员");
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        xssfWorkbook.write(bos);
        xssfWorkbook.close();

        byte[] excelBytes = bos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        // Content-Type: 告诉浏览器响应的内容类型是 Excel 文件
        headers.setContentType(MediaType.
                parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

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
        Student student = userMapper.selectByStudentId(studentId);

        if (student == null) {
            return Result.error(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!BCrypt.checkpw(password, student.getPassword())) {
            return Result.error(MessageConstant.PASSWORD_ERROR);
        }
        StudentVO vo = BeanUtil.toBean(student, StudentVO.class);

        StudentFile avatar = studentFileMapper.selectById(student.getAvatarId());
        if (avatar != null) {
            vo.setAvatar(avatar.getFileUrl());
        }

        BaseContext.setCurrentStudentId(vo.getStudentId());
        String tokenName;
        if (vo.getPosition().equals(JwtClaimsConstant.ADMIN_ID)) {
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
        jwtWhiteList.updateToken(token);
        return Result.success(vo);
    }


    @Override
    public List<StudentVO> getAll() {
        return redisUtil.getAllWithHashCache(CACHE_STUDENTS, this::count, this::queryStudentsFromDB, StudentVO.class);
    }

    public List<StudentVO> queryStudentsFromDB() {
        List<Student> list = this.list();

        // 收集所有非空 avatarId
        Set<Long> avatarIds = list.stream()
                .map(Student::getAvatarId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查头像，构建 id -> url 映射
        Map<Long, String> avatarUrlMap = new HashMap<>();
        if (!avatarIds.isEmpty()) {
            studentFileMapper.selectBatchIds(avatarIds)
                    .forEach(file -> avatarUrlMap.put(file.getId(), file.getFileUrl()));
        }

        return list.stream().map(student -> {
            StudentVO vo = BeanUtil.toBean(student, StudentVO.class);
            Long avatarId = student.getAvatarId();
            if (avatarId != null) {
                vo.setAvatar(avatarUrlMap.get(avatarId));
            }
            Integer studentId = student.getStudentId();
            log.error(String.valueOf(studentId));
            vo.setArticleCount(articleMapper.selectCountByStudentId(studentId));
            vo.setResourceCount(resourcesMapper.selectCountByStudentId(studentId));

            return vo;
        }).toList();
    }

    @Override
    public void removeStudents(List<Integer> studentIds) {
        List<String> list = studentIds.stream().map(String::valueOf).toList();
        Set<String> position = userMapper.selectPositionByIds(studentIds);
        if (position.contains(JwtClaimsConstant.ADMIN_ID)) {
            throw new RuntimeException(MessageConstant.PERMISSION_DENIED);
        }

        jwtWhiteList.deleteToken(list.toArray());
        userMapper.deletebyStudentIds(list);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, list.toArray());

      studentIds.forEach(this::deleteBatch);
    }

    public void deleteBatch(Integer studentId){
        List<Article> articles = articleMapper.selectPageByStudentId(0, studentId, Integer.MAX_VALUE);

        List<String> objectNames = new ArrayList<>();
        for (Article article : articles) {
            Matcher matcher = IMAGE_PATTERN.matcher(article.getContent());
            while (matcher.find()) {
                String url = matcher.group(1);
                objectNames.add(url.substring(url.lastIndexOf('/') + 1));
            }
        }
        commonService.delete(objectNames.toArray(new String[0]));
        articleMapper.deleteByStudentId(studentId);
    }

    @Override
    public void update(Student student) {
        if (student == null) {
            return;
        }
        Integer studentId = student.getStudentId();

        StudentVO vo = this.getUser(BaseContext.getCurrentStudentId());
        //不是管理员却想修改别人
        if (!JwtClaimsConstant.ADMIN_ID.equals(vo.getPosition()) && !vo.getStudentId().equals(studentId)) {
            throw new RuntimeException(MessageConstant.PERMISSION_DENIED);
        }
        String password = student.getPassword();
        if (password != null) {
            student.setPassword(BCrypt.hashpw(password));
        }
        userMapper.updateById(student);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(studentId));
    }

}