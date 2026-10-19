package com.contractcompiler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ContractCompilerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContractCompilerApplication.class, args);
    }
}
