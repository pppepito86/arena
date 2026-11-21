package com.olimpiici.arena.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.service.ProblemService;

public class PublicResourceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ProblemService problemService;

    private PublicResource publicResource;
    private File workDir;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        workDir = folder.newFolder("work");
        when(applicationProperties.getWorkDir()).thenReturn(workDir.getAbsolutePath());

        publicResource = new PublicResource();
        ReflectionTestUtils.setField(publicResource, "applicationProperties", applicationProperties);
        ReflectionTestUtils.setField(publicResource, "problemService", problemService);
    }

    @Test
    public void testGetProblemPdf_UnknownProblemReturns404WithoutWalkingDisk() throws Exception {
        when(problemService.exists(9999L)).thenReturn(false);

        ResponseEntity<?> response = publicResource.getProblemPdf(9999L, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(problemService, never()).getTaskDescription(any(), any());
    }

    @Test
    public void testGetProblemZip_MissingFileReturns404() throws Exception {
        ResponseEntity<?> response = publicResource.getProblemZip(9999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void testGetProblemZip_ExistingFileReturns200() throws Exception {
        File zipFile = new File(workDir, "problems/42/problem.zip");
        FileUtils.writeStringToFile(zipFile, "dummy zip", StandardCharsets.UTF_8);

        ResponseEntity<?> response = publicResource.getProblemZip(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}
