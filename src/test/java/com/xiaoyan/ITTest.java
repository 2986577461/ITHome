package com.xiaoyan;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class ITTest {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();


    @Test
    public void test(){
        System.out.println(encoder.encode("123456"));
    }
}
