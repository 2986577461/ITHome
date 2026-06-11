package com.xiaoyan.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import com.xiaoyan.service.NewcomersService;
import com.xiaoyan.utils.RedisUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import com.xiaoyan.pojo.Newcomer;
import com.xiaoyan.vo.NewcomerVO;

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
    private TransactionTemplate transactionTemplate;
    private RedisUtil redisUtil;
    private UserMapper userMapper;

    @Override
    public void agreeNewcomer(Long id) {
        transactionTemplate.execute(status -> {
            Newcomer newcomer = redisUtil.queryHashWithMutex(CACHE_NEWCOMERS,
                    String.valueOf(id), Newcomer.class, r -> this.getById(id));

            if (newcomer == null) {
                throw new ParameterException(MessageConstant.ACCOUNT_NOT_FOUND);
            }

            Integer studentId = newcomer.getStudentId();

            Student oldStudent = redisUtil.queryHashWithMutex(CACHE_STUDENTS,
                    String.valueOf(studentId), Student.class, s -> userMapper.selectByStudentId(studentId));

            if (oldStudent != null) {
                throw new ParameterException(MessageConstant.REPEATREQUEST);
            }

            this.removeById(id);
            stringRedisTemplate.opsForHash().delete(CACHE_NEWCOMERS, String.valueOf(id));

            Student student = BeanUtil.toBean(newcomer, Student.class);
            student.setPassword(ENCODER.encode(PasswordConstant.STUDENT_PASSWORD));
            student.setPosition(PositionConstant.STUDENT);
            student.setArticleCount(0);
            student.setResourceCount(0);
            student.setAvatarId(1L);

            userMapper.insert(student);

            stringRedisTemplate.opsForHash().put(CACHE_STUDENTS,
                    String.valueOf(studentId), JSONUtil.toJsonStr(student));
            return null;
        });
    }

    @Override
    public void applyJoin(Newcomer newComer) {
        Long id = newComer.getId();

        Newcomer dbNewComer = this.getById(id);
        if (dbNewComer != null) {
            throw new RepeatRuestException(MessageConstant.REPEATREQUEST);
        }

        newComer.setApplicationDateTime(LocalDateTime.now());
        this.save(newComer);
        stringRedisTemplate.opsForHash().put(CACHE_NEWCOMERS, String.valueOf(newComer.getId()),
                JSONUtil.toJsonStr(newComer));
    }

    @Override
    public List<Newcomer> getAll() {
        return redisUtil.getAllWithHashCache(CACHE_NEWCOMERS, this::count,
                this.query()::list, Newcomer.class);
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