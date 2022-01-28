package com.olimpiici.arena.grader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.olimpiici.arena.config.ApplicationProperties;

@Component
public class WorkerPool {

	private List<Worker> workers;

	public WorkerPool(ApplicationProperties applicationProperties) {
		workers = new ArrayList<>();
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
}
