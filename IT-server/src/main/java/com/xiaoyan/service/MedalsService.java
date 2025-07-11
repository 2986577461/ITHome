package com.xiaoyan.service;

import com.xiaoyan.dto.StudentMedalsDTO;
import com.xiaoyan.pojo.StudentMedals;
import com.xiaoyan.vo.StudentMedalsVO;

import java.io.IOException;
import java.util.List;

public interface MedalsService {

    List<StudentMedalsVO> getAll();

    List<StudentMedalsVO> getCurrentUserMedals();

    void save(StudentMedalsDTO studentMedalsDTO) throws IOException;

    void remove(Integer id);
}
