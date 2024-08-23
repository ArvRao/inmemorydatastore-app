package com.inmemorydb.inmemorydb_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InmemorydbAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(InmemorydbAppApplication.class, args);
	}

}
