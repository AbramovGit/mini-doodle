package com.abramovgit.minidoodle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MiniDoodleApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniDoodleApplication.class, args);
    }
}
