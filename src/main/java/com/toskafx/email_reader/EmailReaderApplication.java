package com.toskafx.email_reader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
public class EmailReaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmailReaderApplication.class, args);
	}

}
