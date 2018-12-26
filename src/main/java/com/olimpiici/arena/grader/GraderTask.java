package com.olimpiici.arena.grader;

import java.util.Optional;

import org.pesho.grader.SubmissionScore;
import org.pesho.grader.step.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.service.dto.SubmissionDTO;

import io.jsonwebtoken.io.IOException;

@Component
public class GraderTask {
	
    private final Logger log = LoggerFactory.getLogger(GraderTask.class);

	private ObjectMapper mapper = new ObjectMapper();
    
    private final SubmissionService submissionService;
    private final CompetitionProblemService competitionProblemService;
    public final Worker worker;
    
    public GraderTask(SubmissionService submissionService,
    		CompetitionProblemService competitionProblemService,
    		ApplicationProperties applicationProperties) {
        this.submissionService = submissionService;
        this.competitionProblemService = competitionProblemService;
        this.worker = new Worker(applicationProperties.getWorkerUrl(), applicationProperties.getWorkDir());
    }
    
	@Scheduled(fixedDelay = 3000)
	public void gradeTask() throws IOException {
		if (!worker.isAlive()) {
			log.debug("worker is not alive: " + worker.getUrl());
			return;
		}

		Optional<SubmissionDTO> submission = submissionService.findSubmissionByVerdict("");
		submission.ifPresent(s -> grade(s));
	}
	
	private void grade(SubmissionDTO submission) {
		log.debug("grading submission with id: " + submission.getId());

		submission.setVerdict("judging");
		submissionService.save(submission);
		
		long submissionId = submission.getId();
		long problemId = competitionProblemService.findOne(submission.getCompetitionProblemId()).get().getProblemId();
		
		String result = "";
		String details = "";
		int points = 0;
		try {
			SubmissionScore score = worker.grade(problemId, submissionId);
		
			details = mapper.writeValueAsString(score);
			points = (int) (score.getScore()+0.5);
			StepResult[] values = score.getScoreSteps().values().toArray(new StepResult[0]);
			if (values.length > 1) {
				for (int i = 1; i < values.length; i++) {
					StepResult step = values[i];
					if (i != 1)
						result += ",";
					result += step.getVerdict();
				}
			} else {
				result = values[0].getVerdict().toString();
				System.out.println("Submission <" + submissionId + "> failed with " + values[0].getReason());
}
			
		} catch (Exception e) {
			log.error("scoring failed for submission: " + submissionId, e);
			result = "system error";
		} finally {
			submission.setDetails(details);
			submission.setPoints(points);
			submission.setVerdict(result);
			
			try {
				submissionService.save(submission);
			} catch (Exception e) {
				submission.setVerdict("error");
				log.error("cannot judge submission: " + submission.getId() );
				submissionService.save(submission);
			}
		}
		
	}

}
