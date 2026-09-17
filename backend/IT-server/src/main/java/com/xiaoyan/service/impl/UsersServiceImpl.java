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
import com.xiaoyan.pojo.Resources;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.result.Result;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.utils.TransactionUtils;
import com.xiaoyan.vo.StudentCountVO;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

import static com.xiaoyan.constant.RedisConstant.CACHE_RESOURCES_ALL;
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
    public StudentVO getUser(String studentId) {
        return redisUtil.queryHashWithMutex(CACHE_STUDENTS, studentId,
                StudentVO.class, id -> this.queryStudentFromDB(studentId));
    }

    @Override
    public void checkOwnerOrAdmin(String ownerStudentId) {
        // 权限校验：仅作者本人或管理员可修改
        String currentStudentId = BaseContext.getCurrentStudentId();
        if (currentStudentId == null) {
            throw new ParameterException(Result.UNAUTHORIZED, MessageConstant.USER_NOT_LOGIN);
        }

        StudentVO current = this.getUser(currentStudentId);
        if (current == null) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if (!JwtClaimsConstant.ADMIN_ID.equals(current.getPosition())
                && !currentStudentId.equals(ownerStudentId)) {
            throw new ParameterException(Result.FORBIDDEN, MessageConstant.PERMISSION_DENIED);
        }
    }

    public StudentVO queryStudentFromDB(String studentId) {
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
        String studentId = BaseContext.getCurrentStudentId();
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
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, studentId);
        stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
        // 头像会被烤进文章缓存里的 ArticleVO，不一起失效的话文章列表上还是旧头像
        articleCacheManager.clear();
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
        String studentId = message.getStudentId();
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
        // 空列表要提前返回：下面的 IN () 是非法 SQL
        if (list.isEmpty()) {
            return List.of();
        }

        Set<Long> avatarIds = new HashSet<>();
        List<String> studentIds = new ArrayList<>(list.size());
        for (Student student : list) {
            if (student.getAvatarId() != null) {
                avatarIds.add(student.getAvatarId());
            }
            studentIds.add(student.getStudentId());
        }

        Map<Long, String> avatarUrlMap = new HashMap<>();
        if (!avatarIds.isEmpty()) {
            studentFileMapper.selectBatchIds(avatarIds)
                    .forEach(file -> avatarUrlMap.put(file.getId(), file.getFileUrl()));
        }

        // 原来是在循环里逐个学生 count，一个成员两次查询，50 个成员就是 100 次。
        // 改成按 student_id 分组各查一次，总共 2 次。
        Map<String, Integer> articleCountMap = toCountMap(articleMapper.countByStudentIds(studentIds));
        Map<String, Integer> resourceCountMap = toCountMap(resourcesMapper.countByStudentIds(studentIds));

        return list.stream().map(student -> {
            StudentVO vo = BeanUtil.toBean(student, StudentVO.class);
            if (student.getAvatarId() != null) {
                vo.setAvatar(avatarUrlMap.get(student.getAvatarId()));
            }
            String studentId = student.getStudentId();
            // GROUP BY 只返回有记录的学生，没有文章/资料的查不到，这里默认 0
            vo.setArticleCount(articleCountMap.getOrDefault(studentId, 0));
            vo.setResourceCount(resourceCountMap.getOrDefault(studentId, 0));
            return vo;
        }).toList();
    }

    private static Map<String, Integer> toCountMap(List<StudentCountVO> counts) {
        Map<String, Integer> map = new HashMap<>();
        for (StudentCountVO count : counts) {
            map.put(count.getStudentId(), count.getTotal());
        }
        return map;
    }

    @Override
    @Transactional
    public void removeStudents(List<String> studentIds) {
        if (studentIds == null || studentIds.isEmpty() || studentIds.stream().anyMatch(Objects::isNull)) {
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        }

        List<String> distinctStudentIds = studentIds.stream().distinct().toList();
        Set<String> position = userMapper.selectPositionByIds(distinctStudentIds);
        if (position.contains(JwtClaimsConstant.ADMIN_ID)) {
            throw new ParameterException(Result.FORBIDDEN, MessageConstant.PERMISSION_DENIED);
        }

        removeStudentsInternal(distinctStudentIds);
    }

    @Override
    @Transactional
    public void removeSelf() {
        String currentStudentId = BaseContext.getCurrentStudentId();
        if (currentStudentId == null) {
            throw new ParameterException(Result.UNAUTHORIZED, MessageConstant.USER_NOT_LOGIN);
        }

        StudentVO current = this.getUser(currentStudentId);
        if (current == null) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        // 最后一位管理员不能注销：剩下的都是普通成员，没人能审批新成员、
        // 没人能删人、也没人能改别人的信息，后台会彻底锁死。
        if (JwtClaimsConstant.ADMIN_ID.equals(current.getPosition())
                && userMapper.countByPosition(JwtClaimsConstant.ADMIN_ID) <= 1) {
            throw new ParameterException(MessageConstant.LAST_ADMIN_CANNOT_LEAVE);
        }

        removeStudentsInternal(List.of(currentStudentId));
    }

    /**
     * 真正的删除动作：收集 OSS 文件 → 删库 → 事务提交后清缓存和 OSS。
     *
     * <p>不做权限判断，由调用方负责。注意这是类内部调用、走不到 Spring 代理，
     * 所以 {@code @Transactional} 标在它身上不会生效，必须由带注解的入口方法调用。</p>
     */
    private void removeStudentsInternal(List<String> distinctStudentIds) {
        // 必须先把要删的 OSS 文件全部收集齐再删库：
        // 数据库记录一删，就再也查不出这些文件叫什么了，OSS 上会留下永远清不掉的垃圾。

        // 1) 文章正文里内嵌的图片（从 HTML 里正则提取）
        List<Article> articles = articleMapper.selectByStudentIds(distinctStudentIds);
        Set<String> objectNames = new HashSet<>(extractArticleObjectNames(articles));

        // 2) 该学生上传过的所有文件：头像、文章里的图片、资料的封面和附件
        List<StudentFile> files = new ArrayList<>(
                studentFileMapper.selectByStudentIds(distinctStudentIds));

        // 资料引用的文件归属可能与资料本身不一致（历史数据），按 id 再捞一遍合并，避免漏删
        Set<Long> resourceFileIds = new HashSet<>();
        for (Resources resource : resourcesMapper.selectByStudentIds(distinctStudentIds)) {
            if (resource.getStudentFileCoverId() != null) {
                resourceFileIds.add(resource.getStudentFileCoverId());
            }
            if (resource.getStudentFileFileId() != null) {
                resourceFileIds.add(resource.getStudentFileFileId());
            }
        }
        if (!resourceFileIds.isEmpty()) {
            files.addAll(studentFileMapper.selectBatchIds(resourceFileIds));
        }
        files.forEach(file -> objectNames.add(file.getObjectName()));

        // 删库。文章、资料、文件记录之前都漏了后面两样，导致删完学生之后
        // resources / student_file 里全是查不到主人的孤儿行。
        articleMapper.deleteByStudentIds(distinctStudentIds);
        resourcesMapper.deleteByStudentIds(distinctStudentIds);
        studentFileMapper.deleteByStudentIds(distinctStudentIds);
        userMapper.deletebyStudentIds(distinctStudentIds);

        List<String> objectNameList = new ArrayList<>(objectNames);

        TransactionUtils.afterCommit(() -> {
            jwtWhiteList.deleteToken(distinctStudentIds.toArray());
            stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, distinctStudentIds.toArray());
            stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
            // 资料列表缓存也要失效，否则页面上他发的资料还会在
            stringRedisTemplate.delete(CACHE_RESOURCES_ALL);
            articleCacheManager.clear();

            if (!objectNameList.isEmpty()) {
                try {
                    commonService.delete(objectNameList.toArray(String[]::new));
                } catch (RuntimeException e) {
                    log.error("删除学生文件失败，studentIds={}, objectNames={}",
                            distinctStudentIds, objectNameList, e);
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

        String currentStudentId = BaseContext.getCurrentStudentId();
        StudentVO current = currentStudentId == null ? null : this.getUser(currentStudentId);
        if (current == null) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        boolean isAdmin = JwtClaimsConstant.ADMIN_ID.equals(current.getPosition());
        if (!isAdmin && !currentStudentId.equals(target.getStudentId())) {
            throw new ParameterException(Result.FORBIDDEN, MessageConstant.PERMISSION_DENIED);
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
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS, target.getStudentId());
        stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
        // 文章缓存里的 ArticleVO 带着作者姓名和头像（见 ArticleMapper.xml 的 selectPage），
        // 改了名字不失效的话，列表页会一直显示旧名字直到缓存两小时后过期。
        // 这里不做「有没有真的改」的判断：判空反而更绕，而改资料本来就是低频操作。
        articleCacheManager.clear();
    }

}