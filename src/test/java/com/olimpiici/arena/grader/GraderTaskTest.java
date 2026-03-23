package com.olimpiici.arena.grader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
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

        StepResult compileResult = new StepResult(Verdict.OK);
        score.setCompileResult(compileResult);

        StepResult testResult = new StepResult(Verdict.OK);
        testResult.setTime(0.1);
        score.addTestResult(1, testResult);

        SubmissionDTO submissionDTO = new SubmissionDTO();
        submissionDTO.setId(submissionId);
        when(submissionService.findOne(anyLong())).thenReturn(Optional.of(submissionDTO));

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

        java.util.LinkedHashMap<String, StepResult> scoreSteps = new java.util.LinkedHashMap<>();
        scoreSteps.put("Compile", new StepResult(Verdict.OK));
        StepResult testResult = new StepResult(Verdict.OK);
        testResult.setTime(0.2);
        scoreSteps.put("Test1", testResult);

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

    @Test
    public void testUpdateScore_TruncatesLongVerdict() {
        long submissionId = 1L;
        SubmissionScore score = new SubmissionScore();
        score.addFinalScore(10.0, true);

        for (int i = 1; i <= 200; i++) {
            score.addTestResult(i, new StepResult(Verdict.WAITING));
        }

        SubmissionDTO submissionDTO = new SubmissionDTO();
        submissionDTO.setId(submissionId);
        when(submissionService.findOne(anyLong())).thenReturn(Optional.of(submissionDTO));

        graderTask.updateScore(submissionId, score);

        assertThat(submissionDTO.getVerdict().length()).isLessThanOrEqualTo(500);
    }

    @Test
    public void testUpdateScore_CalculatesMaxTime() {
        long submissionId = 1L;
        SubmissionScore score = new SubmissionScore();
        score.addFinalScore(10.0, true);

        StepResult r1 = new StepResult(Verdict.OK);
        r1.setTime(0.1);
        score.addTestResult(1, r1);

        StepResult r2 = new StepResult(Verdict.OK);
        r2.setTime(0.5);
        score.addTestResult(2, r2);

        StepResult r3 = new StepResult(Verdict.OK);
        r3.setTime(0.3);
        score.addTestResult(3, r3);

        SubmissionDTO submissionDTO = new SubmissionDTO();
        submissionDTO.setId(submissionId);
        when(submissionService.findOne(anyLong())).thenReturn(Optional.of(submissionDTO));

        graderTask.updateScore(submissionId, score);

        assertThat(submissionDTO.getTimeInMillis()).isEqualTo(500);
    }

    @Test
    public void testUpdateScore_UnfinishedScore() {
        long submissionId = 1L;
        SubmissionScore score = new SubmissionScore();
        score.addFinalScore(10.0, false); // finished = false

        StepResult r1 = new StepResult(Verdict.OK);
        score.addTestResult(1, r1);

        SubmissionDTO submissionDTO = new SubmissionDTO();
        submissionDTO.setId(submissionId);
        when(submissionService.findOne(anyLong())).thenReturn(Optional.of(submissionDTO));

        graderTask.updateScore(submissionId, score);

        assertThat(submissionDTO.getVerdict()).isEqualTo("judging");
    }

    @Test
    public void testUpdateScore_HandlesPersistenceFailure() {
        long submissionId = 1L;
        SubmissionScore score = new SubmissionScore();
        score.addFinalScore(10.0, true);

        SubmissionDTO submissionDTO = new SubmissionDTO();
        submissionDTO.setId(submissionId);
        when(submissionService.findOne(anyLong())).thenReturn(Optional.of(submissionDTO));

        // Throw exception on first call to save
        when(submissionService.save(any(SubmissionDTO.class)))
            .thenThrow(new RuntimeException("DB error"))
            .thenReturn(submissionDTO);

        graderTask.updateScore(submissionId, score);

        // Should be called twice: once for the actual update, once for the "error" verdict
        verify(submissionService, times(2)).save(any(SubmissionDTO.class));
        assertThat(submissionDTO.getVerdict()).isEqualTo("error");
    }
}
