package com.olimpiici.arena.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.olimpiici.arena.grader.WorkerPool;
import com.olimpiici.arena.service.AdminAuditService;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.ProblemService;

public class ProblemResourceTest {

    @Mock
    private ProblemService problemService;

    @Mock
    private CompetitionProblemService competitionProblemService;

    @Mock
    private WorkerPool workerPool;

    @Mock
    private AdminAuditService adminAuditService;

    private ProblemResource problemResource;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        problemResource = new ProblemResource(problemService, competitionProblemService);
        ReflectionTestUtils.setField(problemResource, "workerPool", workerPool);
        ReflectionTestUtils.setField(problemResource, "adminAuditService", adminAuditService);
    }

    @Test
    public void testUpdateZip_UnknownProblemReturns404() throws Exception {
        when(problemService.exists(9999L)).thenReturn(false);
        MockMultipartFile file = new MockMultipartFile("file", "problem.zip", "application/zip", new byte[] { 1, 2, 3 });

        ResponseEntity<?> response = problemResource.updateZip(9999L, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(problemService, never()).replaceProblemZip(anyLong(), any());
        verify(workerPool, never()).deleteProblem(anyLong());
    }
}
