package com.xiaoyan.service.impl;

import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.dto.StudentMedalsDTO;
import com.xiaoyan.enumeration.MedalsGradeType;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.exception.PositionException;
import com.xiaoyan.mapper.MedalsMapper;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.mapper.UserMapper;
import com.xiaoyan.pojo.Student;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.pojo.StudentMedals;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.MedalsService;
import com.xiaoyan.vo.StudentMedalsVO;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class MedalsServiceImpl implements MedalsService {

    private MedalsMapper medalsMapper;

    private CommonService commonService;

    private StudentFileMapper studentFileMapper;

    private UserMapper userMapper;

    @Override
    public List<StudentMedalsVO> getAll() {
        return getStudentMedalsVOS(null);
    }

    private List<StudentMedalsVO> getStudentMedalsVOS(Integer studentId) {

        List<StudentMedals> studentMedals = medalsMapper.selectByStudentId(studentId);
        List<StudentMedalsVO> studentMedalsVOS=new ArrayList<>();

        for (StudentMedals medal : studentMedals) {

            StudentMedalsVO medalsVO = new StudentMedalsVO();
            BeanUtils.copyProperties(medal,medalsVO,"grade");
            medalsVO.setGrade(MedalsGradeType.fromCode(medal.getGrade()).getDescription());

            StudentFile studentFile = studentFileMapper.selectById(medal.getStudentFileId());
            medalsVO.setMedalUrl(studentFile.getFileUrl());

            Student student = userMapper.selectByStudentId(medal.getStudentId());
            medalsVO.setStudentName(student.getName());

            studentMedalsVOS.add(medalsVO);
        }
        return studentMedalsVOS;
    }


    @Override
    public List<StudentMedalsVO> getCurrentUserMedals() {
        return this.getStudentMedalsVOS(BaseContext.getCurrentStudentId());
    }

    @Override

    public void save(StudentMedalsDTO studentMedalsDTO) throws IOException {
        StudentMedals studentMedals = new StudentMedals();
        BeanUtils.copyProperties(studentMedalsDTO,studentMedals,"id");

        studentMedals.setStudentId(BaseContext.getCurrentStudentId());

        Integer id = commonService.upload(studentMedalsDTO.getMedalFile());
        studentMedals.setStudentFileId(id);

        studentMedals.setCreateDateTime(LocalDateTime.now());

        medalsMapper.insert(studentMedals);
    }

    @Override
    public void remove(Integer id) {
        StudentMedals studentMedals = medalsMapper.selectById(id);
        if(studentMedals==null)
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);

        Integer studentId = studentMedals.getStudentId();
        if(!Objects.equals(studentId, BaseContext.getCurrentStudentId()))
            throw new PositionException(MessageConstant.ILLEGAL_OPERATION);

        Integer studentFileId = studentMedals.getStudentFileId();
        StudentFile studentFile = studentFileMapper.selectById(studentFileId);
        String objectName = studentFile.getObjectName();
        commonService.delete(objectName);
        medalsMapper.deleteById(id);
        studentFileMapper.deleteById(studentFileId);
    }
}
