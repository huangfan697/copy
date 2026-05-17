package com.wrongnote;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wrongnote.mapper")
public class WrongNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(WrongNoteApplication.class, args);
    }
}
