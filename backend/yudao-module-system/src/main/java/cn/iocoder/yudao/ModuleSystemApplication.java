package cn.iocoder.yudao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"cn.iocoder.yudao"})
public class ModuleSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuleSystemApplication.class, args);
    }

}