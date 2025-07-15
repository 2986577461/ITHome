package com.xiaoyan.service.impl;

import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.AiDialogMapper;
import com.xiaoyan.mapper.AiDialogSessionMapper;
import com.xiaoyan.pojo.AiDialog;
import com.xiaoyan.pojo.AiDialogSession;
import com.xiaoyan.service.AiSessionService;
import com.xiaoyan.vo.AiDialogSessionVO;
import com.xiaoyan.vo.AiDialogVO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class AiSessionServiceImpl implements AiSessionService {

    private AiDialogSessionMapper aiDialogSessionMapper;

    private AiDialogMapper aiDialogMapper;

    @CacheEvict(cacheNames = "sessions", key = "#studentId")
    public Integer createSession(Integer studentId) {
        LocalDateTime now = LocalDateTime.now();

        AiDialogSession session = AiDialogSession.
                builder().
                studentId(studentId).
                createDateTime(now).
                LastActiveDateTime(now).build();

        aiDialogSessionMapper.insert(session);
        return session.getId();
    }

    @Override
    @CacheEvict(value = "sessions", key = "#studentId")
    public void deleteSession(Integer sessionId, Integer studentId) {
        AiDialogSession group = aiDialogSessionMapper.selectById(sessionId);
        if (group == null)
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);

        if (!group.getStudentId().equals(studentId))
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);

        aiDialogSessionMapper.deleteById(sessionId);
        aiDialogMapper.deleteBySessionId(sessionId);
    }

    @Override
    public List<AiDialogVO> getMessages(Integer sessionId) {
        AiDialogSession group = aiDialogSessionMapper.selectById(sessionId);
        if (group == null)
            throw new ParameterException(MessageConstant.RRSOURCES_NO_EXISITS);
        if (!group.getStudentId().equals(BaseContext.getCurrentStudentId()))
            throw new ParameterException(MessageConstant.ILLEGAL_OPERATION);

        List<AiDialog> list = aiDialogMapper.selectBySessionId(sessionId);
        if (list == null)
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);
        List<AiDialogVO> voList = new ArrayList<>();
        for (AiDialog dialog : list) {
            AiDialogVO vo = new AiDialogVO();
            BeanUtils.copyProperties(dialog, vo);
            voList.add(vo);
        }
        voList.sort(Comparator.comparing(AiDialogVO::getCreateDateTime));
        return voList;
    }

    @Cacheable(value = "sessions", key = "#studentId")
    public ArrayList<AiDialogSessionVO> getAll(Integer studentId) {
        ArrayList<AiDialogSession> aiDialogSessions =
                aiDialogSessionMapper.selectByStudentId(studentId);
        ArrayList<AiDialogSessionVO> list = new ArrayList<>();
        for (AiDialogSession group : aiDialogSessions) {
            AiDialogSessionVO vo = new AiDialogSessionVO();
            BeanUtils.copyProperties(group, vo);
            list.add(vo);
        }
        list.sort((o1, o2) -> o2.getLastActiveDateTime().compareTo(o1.getLastActiveDateTime()));
        return list;
    }
}
