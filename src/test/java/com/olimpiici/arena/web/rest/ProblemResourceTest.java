package com.olimpiici.arena.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import com.olimpiici.arena.grader.WorkerPool;
import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.service.AdminAuditService;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.ProblemService;
import com.olimpiici.arena.service.dto.ProblemPackageInfoDTO;

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

    @Test
    public void testUpdateZip_ReturnsPackageInfoDto() throws Exception {
        when(problemService.exists(42L)).thenReturn(true);
        when(competitionProblemService.findAllByProblem(42L)).thenReturn(Collections.emptyList());
        ProblemPackageInfoDTO info = new ProblemPackageInfoDTO();
        info.setHasPackage(true);
        info.setTestCount(20);
        when(problemService.inspectPackage(42L)).thenReturn(info);
        MockMultipartFile file = new MockMultipartFile("file", "problem.zip", "application/zip", new byte[] { 1, 2, 3 });

        ResponseEntity<ProblemPackageInfoDTO> response = problemResource.updateZip(42L, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTestCount()).isEqualTo(20);
        verify(problemService).replaceProblemZip(anyLong(), any());
        verify(workerPool).deleteProblem(42L);
    }

    @Test
    public void testGetPackageInfo_RequiresAdminRoleAndChecksExistence() throws Exception {
        Method method = ProblemResource.class.getMethod("getPackageInfo", Long.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains(AuthoritiesConstants.ADMIN);

        when(problemService.exists(999L)).thenReturn(false);
        assertThat(problemResource.getPackageInfo(999L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        when(problemService.exists(42L)).thenReturn(true);
        ProblemPackageInfoDTO info = new ProblemPackageInfoDTO();
        info.setHasPackage(true);
        when(problemService.inspectPackage(42L)).thenReturn(info);
        ResponseEntity<ProblemPackageInfoDTO> response = problemResource.getPackageInfo(42L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isHasPackage()).isTrue();
    }
}
