package com.xiaoyan.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.constant.PasswordConstant;
import com.xiaoyan.constant.PositionConstant;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.exception.RepeatRuestException;
import com.xiaoyan.mapper.NewcomerMapper;

import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.service.NewcomersService;
import com.xiaoyan.utils.RedisUtil;
import com.xiaoyan.vo.NewcomerVO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static com.xiaoyan.constant.RedisConstant.CACHE_NEWCOMERS;
import static com.xiaoyan.constant.RedisConstant.CACHE_STUDENTS;

/**
 * @author yuchao
 */
@Service
@AllArgsConstructor
@Validated
public class NewcomersServiceImpl extends ServiceImpl<NewcomerMapper, Newcomer>
        implements NewcomersService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private StringRedisTemplate stringRedisTemplate;
    private NewcomerMapper newcomerMapper;
    private TransactionTemplate transactionTemplate;
    private RedisUtil redisUtil;
    private UserMapper userMapper;

    @Override
    public void agreeNewcomer(Long id) {
        transactionTemplate.execute(status -> {
            Object n = stringRedisTemplate.opsForHash().get(CACHE_NEWCOMERS, String.valueOf(id));

            if (n == null) {
                throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
            }

            Newcomer newcomer = JSONUtil.toBean((String) n, Newcomer.class);
            Integer studentId = newcomer.getStudentId();

            Object s = stringRedisTemplate.opsForHash().
                    get(CACHE_STUDENTS, String.valueOf(studentId));
            Student student = JSONUtil.toBean((String) s, Student.class);

            if (student != null) {
                throw new ParameterException(MessageConstant.REPEATREQUEST);
            }

            this.removeById(id);
            stringRedisTemplate.opsForHash().delete(CACHE_NEWCOMERS, String.valueOf(id));

            student = new Student();
            BeanUtils.copyProperties(newcomer, student, "id");
            student.setPassword(ENCODER.encode(PasswordConstant.STUDENT_PASSWORD));
            student.setPosition(PositionConstant.STUDENT);

            userMapper.insert(student);
            stringRedisTemplate.opsForHash().put(CACHE_STUDENTS,
                    String.valueOf(studentId), JSONUtil.toJsonStr(student));
            return null;
        });
    }

    @Override
    public void applyJoin(Newcomer newComer) {
        Long id = newComer.getId();
        Object s = stringRedisTemplate.opsForHash().get(CACHE_NEWCOMERS, String.valueOf(id));
        if (s != null) {
            throw new RepeatRuestException(MessageConstant.REPEATREQUEST);
        }

        newComer.setApplicationDateTime(LocalDateTime.now());
        newcomerMapper.insert(newComer);
        stringRedisTemplate.opsForHash().put(CACHE_NEWCOMERS, String.valueOf(id),
                JSONUtil.toJsonStr(newComer));
    }

    @Override
    public List<NewcomerVO> getAll() {
        return redisUtil.getAllWithHashCache(CACHE_NEWCOMERS, this::count,
                this.query()::list, Newcomer.class, NewcomerVO.class);
    }

    @Override
    public void refuseNewcomer(Long id) {
        if (stringRedisTemplate.opsForHash().get(CACHE_NEWCOMERS, String.valueOf(id)) == null) {
            throw new RuntimeException("申请人不存在！");
        }
        this.removeById(id);
        stringRedisTemplate.opsForHash().delete(CACHE_NEWCOMERS, String.valueOf(id));
    }
}