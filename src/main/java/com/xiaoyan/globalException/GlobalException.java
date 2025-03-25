package com.xiaoyan.globalException;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException
            (RuntimeException pe) {
        return ResponseEntity.status(
                HttpStatus.UNAUTHORIZED).body(pe.getMessage());
    }
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<String> handleException
//            (RuntimeException e) {
//        return ResponseEntity.status(
//                HttpStatus.UNAUTHORIZED).body(e.getAll());
//
//    }

}
