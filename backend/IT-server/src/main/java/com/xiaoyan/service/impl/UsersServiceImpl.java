package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.enumeration.ArticleType;
import com.xiaoyan.exception.ParameterException;
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
import com.xiaoyan.service.PermissionService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.vo.StudentVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

import static com.xiaoyan.constant.RedisConstant.CACHE_ARTICLES;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS_ALL;
import static com.xiaoyan.constant.RedisConstant.RANKING_ARTICLES;

/**
 * @author yuchao
 */
@Service
@AllArgsConstructor
@Slf4j
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
        return userMapper.selectStudentWithStats(studentId);
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
        this.lambdaUpdate().set(Student::getAvatarId, newAvatarId).update();
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(studentId));
        stringRedisTemplate.delete(CACHE_STUDENTS_ALL);

    }

    @Override
    public ResponseEntity<byte[]> downloadExcel() throws IOException {
        List<Student> all = userMapper.selectThisYearsStudents();

        byte[] excelBytes;
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            SXSSFSheet sheet = workbook.createSheet();
            for (int i = 0; i <= 6; i++) {
                sheet.setColumnWidth(i, 4000);
            }

            SXSSFRow titleRow = sheet.createRow(0);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            titleRow.createCell(0).setCellValue("IT之家协会花名册");

            SXSSFRow headerRow = sheet.createRow(1);
            String[] headers = {"学号", "姓名", "性别", "专业", "班级", "学院", "职务"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 2;
            for (Student student : all) {
                if ("AI协会助手".equals(student.getName())) {
                    continue;
                }
                SXSSFRow dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(student.getStudentId());
                dataRow.createCell(1).setCellValue(student.getName());
                dataRow.createCell(2).setCellValue(student.getSex());
                dataRow.createCell(3).setCellValue(student.getMajor());
                dataRow.createCell(4).setCellValue(student.getClassName());
                dataRow.createCell(5).setCellValue(student.getAcademy());
                dataRow.createCell(6).setCellValue("admin".equals(student.getPosition()) ? "会长" : "学员");
            }

            workbook.write(bos);
            excelBytes = bos.toByteArray();
            workbook.dispose();
        }

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
        return redisUtil.getAllWithHashCache(CACHE_STUDENTS, this::queryStudentsFromDB, StudentVO.class);
    }

    public List<StudentVO> queryStudentsFromDB() {
        return userMapper.selectStudentsWithStats();
    }

    @Override
    @Transactional
    public void removeStudents(List<Integer> studentIds) {
        if (studentIds == null || studentIds.isEmpty() || studentIds.stream().anyMatch(Objects::isNull)) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        List<Integer> distinctStudentIds = studentIds.stream().distinct().toList();
        List<String> studentIdStrings = distinctStudentIds.stream().map(String::valueOf).toList();
        Set<String> position = userMapper.selectPositionByIds(distinctStudentIds);
        if (position.contains(JwtClaimsConstant.ADMIN_ID)) {
            throw new ParameterException(MessageConstant.PERMISSION_DENIED);
        }

        List<Article> articles = articleMapper.selectByStudentIds(distinctStudentIds);
        Set<String> objectNames = extractArticleObjectNames(articles);

        articleMapper.deleteByStudentIds(distinctStudentIds);
        userMapper.deletebyStudentIds(studentIdStrings);

        registerAfterCommit(() -> {
            jwtWhiteList.deleteToken(studentIdStrings.toArray());
            stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, studentIdStrings.toArray());
            stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
            clearArticleCache();

            if (!objectNames.isEmpty()) {
                try {
                    commonService.delete(objectNames.toArray(String[]::new));
                } catch (ParameterException e) {
                    log.error("删除学生文章图片失败，studentIds={}", distinctStudentIds, e);
                }
            }
        });
    }

    private Set<String> extractArticleObjectNames(List<Article> articles) {
        Set<String> objectNames = new HashSet<>();
        for (Article article : articles) {
            if (article == null || article.getContent() == null || article.getContent().isEmpty()) {
                continue;
            }
            Matcher matcher = ArticlesServiceImpl.IMAGE_PATTERN.matcher(article.getContent());
            while (matcher.find()) {
                String objectName = matcher.group(1);
                objectNames.add(objectName.substring(objectName.lastIndexOf('/') + 1));
            }
        }
        return objectNames;
    }

    private void registerAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void clearArticleCache() {
        List<String> keys = new ArrayList<>();
        keys.add(CACHE_ARTICLES);
        for (ArticleType articleType : ArticleType.values()) {
            keys.add(RANKING_ARTICLES + ":" + articleType.ordinal());
        }
        keys.add(RANKING_ARTICLES + ":ready");
        stringRedisTemplate.delete(keys);
    }

    @Override
    public void update(Student student) {
        if (student == null) {
            return;
        }
        StudentVO vo = this.getUser(BaseContext.getCurrentStudentId());

        Integer studentId = student.getStudentId();
        //不是管理员却想修改别人
        if (!JwtClaimsConstant.ADMIN_ID.equals(vo.getPosition()) && !vo.getStudentId().equals(studentId)) {
            throw new ParameterException(MessageConstant.PERMISSION_DENIED);
        }
        String password = student.getPassword();
        if (password != null) {
            student.setPassword(BCrypt.hashpw(password));
        }
        userMapper.updateById(student);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(studentId));
        stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
    }

}