package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.cache.ArticleCacheManager;
import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.LoginDTO;
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
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.utils.TransactionUtils;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS_ALL;

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
    private final ArticleCacheManager articleCacheManager;
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

    @Override
    public void checkOwnerOrAdmin(Integer ownerStudentId) {
        // 权限校验：仅作者本人或管理员可修改
        Integer currentStudentId = BaseContext.getCurrentStudentId();
        if (currentStudentId == null) {
            throw new ParameterException(MessageConstant.USER_NOT_LOGIN);
        }

        StudentVO current = this.getUser(currentStudentId);
        if (current == null) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if (!JwtClaimsConstant.ADMIN_ID.equals(current.getPosition())
                && !currentStudentId.equals(ownerStudentId)) {
            throw new ParameterException(MessageConstant.PERMISSION_DENIED);
        }
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
            }
        }
        vo.setArticleCount(articleMapper.selectCountByStudentId(studentId));
        vo.setResourceCount(resourcesMapper.selectCountByStudentId(studentId));
        return vo;
    }

    public void uploadAvatar(MultipartFile avatar) throws IOException {
        Integer studentId = BaseContext.getCurrentStudentId();
        Student student = userMapper.selectByStudentId(studentId);
        if (student == null) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

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

        // avatarId 允许为空，selectById(null) 会直接抛异常
        if (student.getAvatarId() != null) {
            StudentFile avatar = studentFileMapper.selectById(student.getAvatarId());
            if (avatar != null) {
                vo.setAvatar(avatar.getFileUrl());
            }
        }

        BaseContext.setCurrentStudentId(vo.getStudentId());
        String tokenName;
        if (JwtClaimsConstant.ADMIN_ID.equals(vo.getPosition())) {
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
        return redisUtil.queryStringWithMutex(CACHE_STUDENTS_ALL, StudentVO.class, this::queryStudentsFromDB);
    }

    public List<StudentVO> queryStudentsFromDB() {
        List<Student> list = this.list();
        Set<Long> avatarIds = new HashSet<>();
        for (Student student : list) {
            if (student.getAvatarId() != null) {
                avatarIds.add(student.getAvatarId());
            }
        }

        Map<Long, String> avatarUrlMap = new HashMap<>();
        if (!avatarIds.isEmpty()) {
            studentFileMapper.selectBatchIds(avatarIds)
                    .forEach(file -> avatarUrlMap.put(file.getId(), file.getFileUrl()));
        }

        return list.stream().map(student -> {
            StudentVO vo = BeanUtil.toBean(student, StudentVO.class);
            if (student.getAvatarId() != null) {
                vo.setAvatar(avatarUrlMap.get(student.getAvatarId()));
            }
            Integer studentId = student.getStudentId();
            vo.setArticleCount(articleMapper.selectCountByStudentId(studentId));
            vo.setResourceCount(resourcesMapper.selectCountByStudentId(studentId));
            return vo;
        }).toList();
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

        TransactionUtils.afterCommit(() -> {
            jwtWhiteList.deleteToken(studentIdStrings.toArray());
            stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, studentIdStrings.toArray());
            stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
            articleCacheManager.clear();

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

    @Override
    public void update(Student student) {
        if (student == null || student.getId() == null) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        // 归属必须以主键查出来的真实记录为准。
        // 请求里同时带着 id(主键) 和 studentId(学号)：若拿请求传来的 studentId 做校验，
        // 攻击者填上自己的学号 + 别人的主键，校验通过、updateById 却按主键改掉了别人的记录。
        Student target = this.getById(student.getId());
        if (target == null) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        Integer currentStudentId = BaseContext.getCurrentStudentId();
        StudentVO current = currentStudentId == null ? null : this.getUser(currentStudentId);
        if (current == null) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        boolean isAdmin = JwtClaimsConstant.ADMIN_ID.equals(current.getPosition());
        if (!isAdmin && !currentStudentId.equals(target.getStudentId())) {
            throw new ParameterException(MessageConstant.PERMISSION_DENIED);
        }

        // 学号是身份标识，不允许通过这个接口修改
        student.setStudentId(null);
        // 职位只允许管理员改。普通用户传了也置空，updateStrategy=NOT_EMPTY 会把它排除在 UPDATE 之外，
        // 否则任何人都能把自己改成 admin，重新登录就是管理员。
        if (!isAdmin) {
            student.setPosition(null);
        }

        String password = student.getPassword();
        if (password != null) {
            student.setPassword(BCrypt.hashpw(password));
        }

        userMapper.updateById(student);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, String.valueOf(target.getStudentId()));
        stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
    }

}