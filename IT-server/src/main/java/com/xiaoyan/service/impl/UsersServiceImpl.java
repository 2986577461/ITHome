package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.JwtClaimsConstant;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.constant.PositionConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.LoginDTO;
import com.xiaoyan.dto.PasswordDTO;
import com.xiaoyan.exception.AccountNotFoundException;
import com.xiaoyan.exception.PasswordErrorException;
import com.xiaoyan.interceptor.JwtWhiteList;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;

import com.xiaoyan.properties.JwtProperties;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.JwtUtil;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.vo.StudentVO;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

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

    private JwtProperties jwtProperties;
    private StringRedisTemplate stringRedisTemplate;
    private JwtWhiteList jwtWhiteList;
    private RedisUtil redisUtil;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private UserMapper userMapper;

    @Override
    public StudentVO getUser(Integer studentId) {
        Student student = redisUtil.queryHashWithMutex(CACHE_STUDENTS, String.valueOf(studentId),
                Student.class, id -> this.lambdaQuery().eq(Student::getStudentId, id).one());
        if (student == null) {
            return null;
        }
        return BeanUtil.toBean(student, StudentVO.class);
    }

    @Override
    public StudentVO login(LoginDTO message) {
        Integer studentId = message.getStudentId();
        String password = message.getPassword();
        Student student = redisUtil.queryHashWithMutex(CACHE_STUDENTS,
                String.valueOf(studentId), Student.class, id -> this.lambdaQuery().
                        eq(Student::getStudentId, Integer.valueOf(id)).one());
        if (student == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        if (!ENCODER.matches(password, student.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        StudentVO vo = BeanUtil.toBean(student, StudentVO.class);
        BaseContext.setCurrentStudentId(vo.getStudentId());
        String position = vo.getPosition();
        String tokenName;
        if (position.equals(PositionConstant.STUDENT)) {
            tokenName = JwtClaimsConstant.USER_ID;
        } else {
            tokenName = JwtClaimsConstant.ADMIN_ID;
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
        return vo;
    }


    @Override
    public List<StudentVO> getAll() {
        return redisUtil.getAllWithHashCache(CACHE_STUDENTS,
                this::count, this.query()::list, Student.class, StudentVO.class);
    }

    @Override
    public void removeStudents(List<Long> studentIds) {
        userMapper.deleteByIds(studentIds);
        stringRedisTemplate.opsForHash().delete(CACHE_STUDENTS,
                (Object[]) studentIds.stream().map(String::valueOf).toArray(String[]::new));

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