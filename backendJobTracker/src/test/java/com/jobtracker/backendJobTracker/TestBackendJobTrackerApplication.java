package com.jobtracker.backendJobTracker;

import org.springframework.boot.SpringApplication;

public class TestBackendJobTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.from(BackendJobTrackerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
