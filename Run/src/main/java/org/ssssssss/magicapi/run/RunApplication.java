package org.ssssssss.magicapi.run;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.ssssssss.magicapi.utils.XxlJobAdminProperties;

@SpringBootApplication
@EnableConfigurationProperties(XxlJobAdminProperties.class)
public class RunApplication {

    public static void main(String[] args) {
        SpringApplication.run(RunApplication.class, args);
    }

}
