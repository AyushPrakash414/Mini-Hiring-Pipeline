package com.Mini_Hiring_Pipeline.Hiring_pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class HiringPipelineApplication {

	private static final Logger log = LoggerFactory.getLogger(HiringPipelineApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(HiringPipelineApplication.class, args);
	}

	@Bean
	public CommandLineRunner schemaMigrationRunner(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				log.info("Running schema safety migrations...");
				jdbcTemplate.execute("ALTER TABLE candidates ADD COLUMN IF NOT EXISTS stage_started_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()");
				jdbcTemplate.execute("UPDATE candidates SET stage_started_at = created_at WHERE stage_started_at IS NULL");
				log.info("Schema safety migrations completed successfully.");
			} catch (Exception e) {
				log.warn("Schema migration notice: {}", e.getMessage());
			}
		};
	}
}
