package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.constant.PasswordConstant;
import com.xiaoyan.constant.PositionConstant;

import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.NewcomerMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.service.NewcomersService;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.validation.annotation.Validated;
import com.xiaoyan.pojo.Newcomer;

import java.time.LocalDateTime;
import java.util.List;

import static com.xiaoyan.constant.RedisConstant.CACHE_NEWCOMERS;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS_ALL;

/**
 * @author yuchao
 */
@Service
@AllArgsConstructor
@Validated
public class NewcomersServiceImpl extends ServiceImpl<NewcomerMapper, Newcomer>
        implements NewcomersService {

    private final NewcomerMapper newcomerMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private UserMapper userMapper;

    @Override
    @Transactional
    public void agreeNewcomer(Long id) {

        // 审批是写业务，必须以数据库为准，并锁住当前申请记录
        Newcomer newcomer = newcomerMapper.selectByIdForUpdate(id);

        if (newcomer == null) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        Integer studentId = newcomer.getStudentId();

        // 不能使用缓存判断学生是否存在，否则缓存未更新时可能重复创建
        Student oldStudent = userMapper.selectByStudentId(studentId);
        if (oldStudent != null) {
            throw new ParameterException(MessageConstant.REPEATREQUEST);
        }

        int deletedRows = newcomerMapper.deleteById(id);
        if (deletedRows != 1) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        Student student = BeanUtil.toBean(newcomer, Student.class);
        student.setPassword(BCrypt.hashpw((PasswordConstant.STUDENT_PASSWORD)));
        student.setPosition(PositionConstant.STUDENT);
        student.setAvatarId(1L);

        userMapper.insert(student);
        stringRedisTemplate.delete(CACHE_STUDENTS_ALL);
        // 事务提交成功后再删除缓存，避免数据库回滚但缓存已失效
        stringRedisTemplate.opsForHash().delete(CACHE_NEWCOMERS, String.valueOf(id));
    }

    @Override
    @Transactional
    public void applyJoin(@NonNull Newcomer newComer) {
        Integer studentId = newComer.getStudentId();

        Newcomer dbNewComer = newcomerMapper.selectByStudentId(studentId);
        Student dbStudent = userMapper.selectByStudentId(studentId);
        if (dbStudent != null || dbNewComer != null) {
            throw new ParameterException(MessageConstant.REPEATREQUEST);
        }

        newComer.setApplicationDateTime(LocalDateTime.now());
        try {
            if (!this.save(newComer)) {
                throw new ParameterException(MessageConstant.PARAMETER_ERROR);
            }
        } catch (DuplicateKeyException e) {
            // 并发请求由数据库唯一索引兜底，并转换为统一业务异常
            throw new ParameterException(MessageConstant.REPEATREQUEST);
        }
    }

    @Override
    public List<Newcomer> getAll() {
        return this.list();
    }

    @Override
    @Transactional
    public void refuseNewcomer(Long id) {
        Newcomer newcomer = newcomerMapper.selectByIdForUpdate(id);

        if (newcomer == null) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        int deletedRows = newcomerMapper.deleteById(id);
        if (deletedRows != 1) {
            throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

    }
}