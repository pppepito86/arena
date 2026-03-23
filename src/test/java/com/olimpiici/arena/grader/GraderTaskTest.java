package com.olimpiici.arena.grader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pesho.grader.SubmissionScore;
import org.pesho.grader.step.StepResult;
import org.pesho.grader.step.Verdict;

import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.service.dto.SubmissionDTO;

public class GraderTaskTest {

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private GraderTask graderTask;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdateScoreWithNullScoreSteps() {
        long submissionId = 1L;
        SubmissionScore score = new SubmissionScore();
        score.addFinalScore(10.0, true);

        // Mocking the modern structure
        StepResult compileResult = new StepResult(Verdict.OK);
        score.setCompileResult(compileResult);

        StepResult testResult = new StepResult(Verdict.OK);
        testResult.setTime(0.1);
        score.addTestResult(1, testResult);

        // Ensure getScoreSteps() returns null as in the reported issue
        // (It returns null by default in the current implementation)

        SubmissionDTO submissionDTO = new SubmissionDTO();
        submissionDTO.setId(submissionId);
        when(submissionService.findOne(anyLong())).thenReturn(Optional.of(submissionDTO));

        // This should not throw NullPointerException
        graderTask.updateScore(submissionId, score);

        verify(submissionService).save(submissionDTO);
        assertThat(submissionDTO.getVerdict()).isEqualTo("OK");
    }

    @Test
    public void testUpdateScoreWithCompileError() {
        long submissionId = 1L;
        SubmissionScore score = new SubmissionScore();
        score.addFinalScore(0.0, true);

        StepResult compileResult = new StepResult(Verdict.CE, "Compile error");
        score.setCompileResult(compileResult);

        SubmissionDTO submissionDTO = new SubmissionDTO();
        submissionDTO.setId(submissionId);
        when(submissionService.findOne(anyLong())).thenReturn(Optional.of(submissionDTO));

        graderTask.updateScore(submissionId, score);

        verify(submissionService).save(submissionDTO);
        assertThat(submissionDTO.getVerdict()).isEqualTo("CE");
    }

    @Test
    public void testUpdateScoreWithLegacyFormat() throws Exception {
        long submissionId = 1L;
        SubmissionScore score = new SubmissionScore();
        score.addFinalScore(10.0, true);

        // Legacy format: everything in scoreSteps
        java.util.LinkedHashMap<String, StepResult> scoreSteps = new java.util.LinkedHashMap<>();
        scoreSteps.put("Compile", new StepResult(Verdict.OK));
        StepResult testResult = new StepResult(Verdict.OK);
        testResult.setTime(0.2);
        scoreSteps.put("Test1", testResult);

        // Use reflection to set the private field scoreSteps
        java.lang.reflect.Field field = SubmissionScore.class.getDeclaredField("scoreSteps");
        field.setAccessible(true);
        field.set(score, scoreSteps);

        SubmissionDTO submissionDTO = new SubmissionDTO();
        submissionDTO.setId(submissionId);
        when(submissionService.findOne(anyLong())).thenReturn(Optional.of(submissionDTO));

        graderTask.updateScore(submissionId, score);

        verify(submissionService).save(submissionDTO);
        assertThat(submissionDTO.getVerdict()).isEqualTo("OK");
    }
}
