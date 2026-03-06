package com.olimpiici.arena.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.grader.WorkerPool;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.mapper.ProblemMapper;
import com.olimpiici.arena.service.mapper.TagMapper;

public class ProblemServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private ProblemRepository problemRepository;
    @Mock
    private ProblemMapper problemMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private TagService tagService;
    @Mock
    private CompetitionProblemRepository competitionProblemRepository;
    @Mock
    private SubmissionService submissionService;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private UserService userService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private WorkerPool workerPool;

    private ProblemService problemService;

    private File workDir;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        workDir = folder.newFolder("work");
        when(applicationProperties.getWorkDir()).thenReturn(workDir.getAbsolutePath());

        problemService = new ProblemService(
            problemRepository,
            problemMapper,
            tagMapper,
            tagService
        );

        ReflectionTestUtils.setField(problemService, "competitionProblemRepository", competitionProblemRepository);
        ReflectionTestUtils.setField(problemService, "submissionService", submissionService);
        ReflectionTestUtils.setField(problemService, "submissionRepository", submissionRepository);
        ReflectionTestUtils.setField(problemService, "userService", userService);
        ReflectionTestUtils.setField(problemService, "applicationProperties", applicationProperties);
        ReflectionTestUtils.setField(problemService, "workerPool", workerPool);
    }

    @Test
    public void testGetProperties_DefaultValues() {
        Properties props = problemService.getProperties(1L);
        assertThat(props.getProperty("time")).isEqualTo("1");
        assertThat(props.getProperty("memory")).isEqualTo("256");
    }

    @Test
    public void testGetProperties_FileExists() throws IOException {
        File problemDir = new File(workDir, "problems/1/problem");
        problemDir.mkdirs();
        File gradeProps = new File(problemDir, "grade.properties");
        Properties initial = new Properties();
        initial.setProperty("time", "2.5");
        initial.setProperty("memory", "512");
        try (FileOutputStream fos = new FileOutputStream(gradeProps)) {
            initial.store(fos, null);
        }

        Properties result = problemService.getProperties(1L);
        assertThat(result.getProperty("time")).isEqualTo("2.5");
        assertThat(result.getProperty("memory")).isEqualTo("512");
    }

    @Test
    public void testDelete_HasUserSubmissions() throws IOException {
        User user = new User();
        user.setId(100L); // Regular user
        Submission submission = new Submission();
        submission.setUser(user);

        CompetitionProblem cp = new CompetitionProblem();
        when(competitionProblemRepository.findByProblemId(1L)).thenReturn(Arrays.asList(cp));
        when(submissionRepository.findAllByCompetitionProblem(cp)).thenReturn(Arrays.asList(submission));
        when(userService.isUserAdmin(user)).thenReturn(false);

        assertThatThrownBy(() -> problemService.delete(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("user submissions");
    }

    @Test
    public void testDelete_OnlyAdminSubmissions() throws IOException {
        User admin = new User();
        admin.setId(100L);
        Submission submission = new Submission();
        submission.setId(123L);
        submission.setUser(admin);

        Problem problem = new Problem();
        problem.setId(1L);
        CompetitionProblem cp = new CompetitionProblem();
        cp.setProblem(problem);

        when(competitionProblemRepository.findByProblemId(1L)).thenReturn(Arrays.asList(cp));
        when(submissionRepository.findAllByCompetitionProblem(cp)).thenReturn(Arrays.asList(submission));
        when(userService.isUserAdmin(admin)).thenReturn(true);
        when(problemRepository.getOne(1L)).thenReturn(problem);

        problemService.delete(1L);

        verify(submissionService).delete(123L);
        verify(problemRepository).deleteById(1L);
        verify(workerPool).deleteProblem(1L);
    }

    @Test
    public void testPopulateCompetitionInfo() {
        Competition root = new Competition();
        root.setId(1L);
        root.setLabel("Root");

        Competition yearComp = new Competition();
        yearComp.setId(2L);
        yearComp.setLabel("2020");
        yearComp.setParent(root);

        Competition nameComp = new Competition();
        nameComp.setId(3L);
        nameComp.setLabel("National Olympiad");
        nameComp.setParent(yearComp);

        Competition groupComp = new Competition();
        groupComp.setId(4L);
        groupComp.setLabel("A");
        groupComp.setParent(nameComp);

        Problem problem = new Problem();
        problem.setId(10L);

        CompetitionProblem cp = new CompetitionProblem();
        cp.setId(100L);
        cp.setCompetition(groupComp);
        cp.setProblem(problem);

        when(competitionProblemRepository.findAll()).thenReturn(Arrays.asList(cp));

        problemService.populateCompetitionInfo();

        assertThat(problem.getYear()).isEqualTo(2020);
        assertThat(problem.getGroup()).isEqualTo("A");
        assertThat(problem.getCompetition()).isEqualTo(nameComp);
        assertThat(problem.getCanonicalCompetitionProblem()).isEqualTo(cp);
        verify(problemRepository).save(problem);
    }

    @Test
    public void testSetLimitsToDto() throws IOException {
        ProblemDTO dto = new ProblemDTO();
        dto.setId(1L);

        File problemDir = new File(workDir, "problems/1/problem");
        problemDir.mkdirs();
        File gradeProps = new File(problemDir, "grade.properties");
        Properties initial = new Properties();
        initial.setProperty("time", "1.5");
        initial.setProperty("memory", "128");
        try (FileOutputStream fos = new FileOutputStream(gradeProps)) {
            initial.store(fos, null);
        }

        ProblemDTO result = problemService.setLimitsToDto(dto);

        assertThat(result.getTime()).isEqualTo(1500);
        assertThat(result.getMemory()).isEqualTo(128);
    }
}
