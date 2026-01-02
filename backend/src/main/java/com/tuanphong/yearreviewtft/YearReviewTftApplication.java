package com.tuanphong.yearreviewtft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class YearReviewTftApplication {

	public static void main(String[] args) {
		SpringApplication.run(YearReviewTftApplication.class, args);
	}

}
