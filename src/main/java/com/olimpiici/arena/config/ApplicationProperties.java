package com.olimpiici.arena.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Arena.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

	private String workDir = ".";
	private String workerUrl = "localhost:8089";

	public String getWorkDir() {
		return workDir;
	}
	
	public void setWorkDir(String workDir) {
		this.workDir = workDir;
	}
	
	public String getWorkerUrl() {
		return workerUrl;
	}
	
	public void setWorkerUrl(String workerUrl) {
		this.workerUrl = workerUrl;
	}
	
}
