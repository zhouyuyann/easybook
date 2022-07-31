package com.yuyanzhou.easy_book;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class EasyBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyBookApplication.class, args);
    }

    @GetMapping("/hello")
    public String hello(){
        return "hello sprint boot";
    }
}
