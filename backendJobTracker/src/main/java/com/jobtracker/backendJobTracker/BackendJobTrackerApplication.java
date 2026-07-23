package com.jobtracker.backendJobTracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.jobtracker.backendJobTracker.config.JobTrackerProperties;

@SpringBootApplication
@EnableConfigurationProperties(JobTrackerProperties.class)
public class BackendJobTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendJobTrackerApplication.class, args);
	}

}
