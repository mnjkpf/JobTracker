package com.jobtracker.backendJobTracker;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		// pgvector-образ (не звичайний postgres) — міграції V6/V12 роблять
		// CREATE EXTENSION vector та створюють vector(1536)-колонки з HNSW-індексами.
		// На чистому postgres:latest Flyway впав би і жоден IT не стартував.
		return new PostgreSQLContainer<>(
				DockerImageName.parse("pgvector/pgvector:pg18")
						.asCompatibleSubstituteFor("postgres"));
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);
	}

}
