package com.olimpiici.arena.grader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.olimpiici.arena.config.ApplicationProperties;

@Component
public class WorkerPool {
	private final Logger log = LoggerFactory.getLogger(WorkerPool.class);
	private List<Worker> workers;

	public WorkerPool(ApplicationProperties applicationProperties) {
		workers = new ArrayList<>();

		if (applicationProperties.getWorkerUrl().isEmpty()) {
			log.error("Worker URL is not configured.");
			return;
		}
		workers.add(new Worker(applicationProperties.getWorkerUrl(), applicationProperties.getWorkDir()));

	}

	public void deleteProblem(long problemId) {
		workers.forEach(worker -> {
			try {
				worker.deleteProblem(problemId);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public Worker getOne() {
		return workers.get(0);
	}

	public boolean isAlive() {
		if (workers.isEmpty()) {
			return false;
		}

		return getOne().isAlive();
	}
}
