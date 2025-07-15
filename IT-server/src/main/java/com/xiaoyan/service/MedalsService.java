package com.xiaoyan.service;

import com.xiaoyan.dto.StudentMedalsDTO;
import com.xiaoyan.vo.StudentMedalsVO;

import java.io.IOException;
import java.util.List;

public interface MedalsService {

    List<StudentMedalsVO> getAll();

    List<StudentMedalsVO> getUserMedals(Integer studentId);

    void save(StudentMedalsDTO studentMedalsDTO) throws IOException;

    void remove(Long id,Integer studentId);
}
