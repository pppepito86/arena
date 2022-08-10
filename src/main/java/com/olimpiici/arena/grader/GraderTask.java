package com.olimpiici.arena.grader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.pesho.grader.SubmissionScore;
import org.pesho.grader.step.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.service.dto.SubmissionDTO;

import io.jsonwebtoken.io.IOException;

@Component
public class GraderTask {

    private final Logger log = LoggerFactory.getLogger(GraderTask.class);


	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
    private SubmissionService submissionService;

    @Autowired
    private CompetitionProblemService competitionProblemService;

    @Autowired
    public WorkerPool workerPool;


	@Scheduled(fixedDelay = 3000)
	public void gradeTask() throws IOException {
		List<SubmissionDTO> submissions = new ArrayList<>();
		submissions.addAll(submissionService.findSubmissionByVerdict("judging"));
		submissions.addAll(submissionService.findSubmissionByVerdict("waiting"));

		if (submissions.isEmpty()) return;

		if (!workerPool.isAlive()) {
			log.error("Worker pool is not alive.");
			return;
		}

		submissions.forEach(s -> grade(s));
	}

	private void grade(SubmissionDTO submission) {
		log.debug("grading submission with id: " + submission.getId());

		submission.setVerdict("judging");
		submissionService.save(submission);

		long submissionId = submission.getId();
		long problemId = competitionProblemService.findOne(submission.getCompetitionProblemId()).get().getProblemId();

		SubmissionScore score = new SubmissionScore();
		try {
			boolean isAuthor = submission.getUserId() == 4;
			score = workerPool.getOne().grade(problemId, submissionId, this, isAuthor);
		} catch (Exception e) {
			log.error("scoring failed for submission: " + submissionId, e);
			score.addFinalScore("system error", 0);
		} finally {
			updateScore(submissionId, score);
		}
	}

	public void updateScore(long submissionId, SubmissionScore score) {
		String verdict = "";
		String details = "";
		int points = 0;
		double time = 0;
		try {
			details = mapper.writeValueAsString(score);
			points = (int) (score.getScore()+0.5);
			StepResult[] values = score.getScoreSteps().values().toArray(new StepResult[0]);
			if (values.length > 1) {
				for (int i = 1; i < values.length; i++) {
					StepResult step = values[i];
					if (i != 1)
						verdict += ",";
					verdict += step.getVerdict();
					if (step.getTime() != null) {
						time = Math.max(time, step.getTime());
					}
				}
			} else if (values.length == 1){
				verdict = values[0].getVerdict().toString();
                if (!verdict.equals("OK")) {
				    System.out.println("Submission <" + submissionId + "> failed with "
                        + values[0].getReason());
                }
			}
		} catch (Exception e) {
			log.error("scoring failed for submission: " + submissionId, e);
			verdict = "system error";
		} finally {
			verdict = verdict.substring(0, Math.min(verdict.length(), 500));
			Optional<SubmissionDTO> maybeSubmission = submissionService.findOne(submissionId);
			if (!maybeSubmission.isPresent()) return;

			SubmissionDTO submission = maybeSubmission.get();
			submission.setDetails(details);
			submission.setPoints(points);
			if (score.isFinished()) submission.setVerdict(verdict);
			else submission.setVerdict("judging");
			submission.setTimeInMillis((int) (1000*time+0.5));

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
