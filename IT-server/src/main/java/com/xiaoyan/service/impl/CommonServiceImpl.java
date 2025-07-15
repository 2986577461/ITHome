package com.xiaoyan.service.impl;

import com.xiaoyan.constant.MessageConstant;
import com.xiaoyan.context.BaseContext;
import com.xiaoyan.exception.ParameterException;
import com.xiaoyan.mapper.StudentFileMapper;
import com.xiaoyan.pojo.StudentFile;
import com.xiaoyan.service.CommonService;
import com.xiaoyan.service.UsersService;
import com.xiaoyan.utils.AliOssUtil;
import com.xiaoyan.vo.StudentVO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@AllArgsConstructor
public class CommonServiceImpl implements CommonService {

    private AliOssUtil aliOssUtil;

    private StudentFileMapper studentFileMapper;

    private UsersService usersService;

    @Override
    public Long upload(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename();
        if (originalName == null)
            throw new ParameterException(MessageConstant.PARAMETER_ERROR);

        String suffix = originalName.substring(originalName.lastIndexOf("."));

        String objectName = UUID.randomUUID() + suffix;
        String fileUrl = aliOssUtil.upload(file.getBytes(), objectName);

        long size = file.getSize();
        String contentType = file.getContentType();

        StudentFile record = StudentFile.builder().
                studentId(BaseContext.getCurrentStudentId()).
                fileSize(size).
                originalName(originalName).
                objectName(objectName).
                fileType(contentType).
                createDateTime(LocalDateTime.now()).
                fileUrl(fileUrl).build();

        studentFileMapper.insert(record);

        //回显id
        return record.getId();
    }

    @Override
    public void delete(String objectName) {
        aliOssUtil.delete(objectName);
        studentFileMapper.deleteByObjectName(objectName);
    }

    /**
     * 生成带签名的下载URL
     *
     * @param objectName       OSS上的文件路径/名称
     * @param expirationMillis URL的过期时间（毫秒），例如 3600 * 1000 = 1小时
     * @return 带签名的下载URL
     */
    public String generatePresignedDownloadUrl(String objectName, long expirationMillis) {
        StudentFile studentFile = studentFileMapper.selectbyObjectName(objectName);
        log.info("下载文件:{}",studentFile.getOriginalName());
        return aliOssUtil.getDownloadUrl(objectName,
                studentFile.getOriginalName(), expirationMillis);
    }
    @Override
    public ResponseEntity<byte[]> downloadExcel() throws IOException {
        List<StudentVO> all = usersService.getAll();
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        XSSFSheet sheet = xssfWorkbook.createSheet();
        for (int i = 0; i <= 6; i++) {
            sheet.setColumnWidth(i, 4000);
        }

        XSSFRow row = sheet.createRow(0);
        //合并单元格
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        row.createCell(0).setCellValue("IT之家协会花名册");

        XSSFRow row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("学号");
        row1.createCell(1).setCellValue("姓名");
        row1.createCell(2).setCellValue("性别");
        row1.createCell(3).setCellValue("专业");
        row1.createCell(4).setCellValue("班级");
        row1.createCell(5).setCellValue("学院");
        row1.createCell(6).setCellValue("职务");

        for (int i = 2, s = 0; s < all.size(); i++, s++) {
            XSSFRow row2 = sheet.createRow(i);
            row2.createCell(0).setCellValue(all.get(s).getStudentId());
            row2.createCell(1).setCellValue(all.get(s).getName());
            row2.createCell(2).setCellValue(all.get(s).getSex());
            row2.createCell(3).setCellValue(all.get(s).getMajor());
            row2.createCell(4).setCellValue(all.get(s).getClassName());
            row2.createCell(5).setCellValue(all.get(s).getAcademy());
            row2.createCell(6).setCellValue(all.get(s).getPosition());
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        xssfWorkbook.write(bos);
        xssfWorkbook.close();

        byte[] excelBytes = bos.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        // Content-Type: 告诉浏览器响应的内容类型是 Excel 文件
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml" +
                ".sheet"));

        // Content-Disposition: 告诉浏览器这是一个附件，并指定下载的文件名
        String fileName = "IT之家协会花名册.xlsx";
        // filename* 参数用于 UTF-8 编码的文件名，优先被现代浏览器识别
        // filename 参数用于兼容旧浏览器，通常使用 ISO-8859-1 编码（或直接使用原始字符串，由浏览器自行处理）
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20"); // URL编码并处理空格
        String contentDisposition =
                "attachment; filename=\"" + encodedFileName + "\"; filename*=utf-8''" + encodedFileName;

        headers.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);

        // Content-Length: 告诉浏览器文件大小，有助于下载进度显示
        headers.setContentLength(excelBytes.length);
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
