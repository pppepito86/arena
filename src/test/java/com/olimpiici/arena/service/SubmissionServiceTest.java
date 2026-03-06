package com.olimpiici.arena.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.CompetitionRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.mapper.SubmissionMapper;
import com.olimpiici.arena.service.mapper.TagMapper;
import com.olimpiici.arena.web.rest.errors.BadRequestAlertException;

public class SubmissionServiceTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private CompetitionRepository competitionRepository;
    @Mock
    private CompetitionProblemRepository competitionProblemRepository;
    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private TagService tagService;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private ProblemService problemService;

    private SubmissionService submissionService;

    private File workDir;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        workDir = folder.newFolder("work");
        when(applicationProperties.getWorkDir()).thenReturn(workDir.getAbsolutePath());

        submissionService = new SubmissionService(
            submissionRepository,
            submissionMapper,
            competitionProblemRepository,
            userRepository,
            competitionRepository,
            applicationProperties,
            tagService,
            tagMapper
        );

        // Handle @Autowired fields
        ReflectionTestUtils.setField(submissionService, "userService", userService);
        ReflectionTestUtils.setField(submissionService, "problemService", problemService);
    }

    @Test
    public void testMaybeBanSubmission_AlreadyBanned() throws IOException {
        Submission submission = new Submission();
        submission.setVerdict("banned");

        boolean result = submissionService.maybeBanSubmission(submission);

        assertThat(result).isFalse();
        verify(submissionRepository, never()).save(any());
    }

    @Test
    public void testMaybeBanSubmission_ZeroPoints() throws IOException {
        Submission submission = new Submission();
        submission.setPoints(0);

        boolean result = submissionService.maybeBanSubmission(submission);

        assertThat(result).isFalse();
        verify(submissionRepository, never()).save(any());
    }

    @Test
    public void testMaybeBanSubmission_UserExempt() throws IOException {
        User user = new User();
        Submission submission = new Submission();
        submission.setPoints(100);
        submission.setUser(user);
        when(userService.isUserExemptFromPolicyChecks(user)).thenReturn(true);

        boolean result = submissionService.maybeBanSubmission(submission);

        assertThat(result).isFalse();
        verify(submissionRepository, never()).save(any());
    }

    @Test
    public void testMaybeBanSubmission_MatchesAuthor() throws IOException {
        User user = new User();
        Problem problem = new Problem();
        problem.setId(1L);
        CompetitionProblem cp = new CompetitionProblem();
        cp.setProblem(problem);

        Submission submission = new Submission();
        submission.setId(123L);
        submission.setPoints(100);
        submission.setUser(user);
        submission.setCompetitionProblem(cp);

        // Setup files
        File subDir = new File(workDir, "submissions/123");
        subDir.mkdirs();
        File subFile = new File(subDir, "solution.cpp");
        FileUtils.writeStringToFile(subFile, "int main() { return 0; }", StandardCharsets.UTF_8);

        File authorFile = folder.newFile("author.cpp");
        FileUtils.writeStringToFile(authorFile, "int main() { \n return 0; \n }", StandardCharsets.UTF_8);

        when(userService.isUserExemptFromPolicyChecks(user)).thenReturn(false);
        when(problemService.getAuthorSolution(workDir.getAbsolutePath(), 1L))
            .thenReturn(Optional.of(authorFile));

        boolean result = submissionService.maybeBanSubmission(submission);

        assertThat(result).isTrue();
        assertThat(submission.getVerdict()).isEqualTo("banned");
        assertThat(submission.getPoints()).isEqualTo(0);
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testMaybeBanSubmission_NoMatch() throws IOException {
        User user = new User();
        Problem problem = new Problem();
        problem.setId(1L);
        CompetitionProblem cp = new CompetitionProblem();
        cp.setProblem(problem);

        Submission submission = new Submission();
        submission.setId(123L);
        submission.setPoints(100);
        submission.setUser(user);
        submission.setCompetitionProblem(cp);

        // Setup files
        File subDir = new File(workDir, "submissions/123");
        subDir.mkdirs();
        File subFile = new File(subDir, "solution.cpp");
        FileUtils.writeStringToFile(subFile, "user code", StandardCharsets.UTF_8);

        File authorFile = folder.newFile("author.cpp");
        FileUtils.writeStringToFile(authorFile, "author code", StandardCharsets.UTF_8);

        when(userService.isUserExemptFromPolicyChecks(user)).thenReturn(false);
        when(problemService.getAuthorSolution(workDir.getAbsolutePath(), 1L))
            .thenReturn(Optional.of(authorFile));

        boolean result = submissionService.maybeBanSubmission(submission);

        assertThat(result).isFalse();
        verify(submissionRepository, never()).save(submission);
    }

    @Test
    public void testMaybeThrottleSubmission_NotActivated() {
        User user = new User();
        user.setId(1L);
        user.setActivated(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userService.isUserExemptFromPolicyChecks(user)).thenReturn(false);

        assertThatThrownBy(() -> submissionService.maybeThrottleSubmission(1L))
            .isInstanceOf(BadRequestAlertException.class)
            .hasFieldOrPropertyWithValue("errorKey", "account-not-activated");
    }

    @Test
    public void testMaybeThrottleSubmission_TooFast() {
        User user = new User();
        user.setId(1L);
        user.setActivated(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userService.isUserExemptFromPolicyChecks(user)).thenReturn(false);

        Timestamp lastSubmit = Timestamp.from(Instant.now().minusSeconds(5));
        when(submissionRepository.findLastByUser(1L)).thenReturn(Optional.of(lastSubmit));

        assertThatThrownBy(() -> submissionService.maybeThrottleSubmission(1L))
            .isInstanceOf(BadRequestAlertException.class)
            .hasFieldOrPropertyWithValue("errorKey", "submission-throttled");
    }

    @Test
    public void testMaybeThrottleSubmission_TooMany() {
        User user = new User();
        user.setId(1L);
        user.setActivated(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userService.isUserExemptFromPolicyChecks(user)).thenReturn(false);

        Timestamp lastSubmit = Timestamp.from(Instant.now().minusSeconds(60));
        when(submissionRepository.findLastByUser(1L)).thenReturn(Optional.of(lastSubmit));
        when(submissionRepository.numSubmissionsLastDay(1L)).thenReturn(200);

        assertThatThrownBy(() -> submissionService.maybeThrottleSubmission(1L))
            .isInstanceOf(BadRequestAlertException.class)
            .hasFieldOrPropertyWithValue("errorKey", "too-many-submissions-per-day");
    }

    @Test
    public void testMaybeThrottleSubmission_Success() {
        User user = new User();
        user.setId(1L);
        user.setActivated(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userService.isUserExemptFromPolicyChecks(user)).thenReturn(false);

        Timestamp lastSubmit = Timestamp.from(Instant.now().minusSeconds(60));
        when(submissionRepository.findLastByUser(1L)).thenReturn(Optional.of(lastSubmit));
        when(submissionRepository.numSubmissionsLastDay(1L)).thenReturn(10);

        submissionService.maybeThrottleSubmission(1L);
        // No exception thrown
    }

    @Test
    public void testRejudge() {
        Submission submission = new Submission();
        submission.setId(1L);
        submission.setVerdict("wrong");
        when(submissionRepository.getOne(1L)).thenReturn(submission);

        submissionService.rejudge(1L);

        assertThat(submission.getVerdict()).isEqualTo("waiting");
        verify(submissionRepository).save(submission);
    }

    @Test
    public void testFindSubmissionCode() throws IOException {
        File subDir = new File(workDir, "submissions/123");
        subDir.mkdirs();
        File subFile = new File(subDir, "solution.cpp");
        String code = "sample code";
        FileUtils.writeStringToFile(subFile, code, StandardCharsets.UTF_8);

        String result = submissionService.findSubmissionCode(123L);

        assertThat(result).isEqualTo(code);
    }

    @Test
    public void testDelete() throws IOException {
        File subDir = new File(workDir, "submissions/123");
        subDir.mkdirs();
        File subFile = new File(subDir, "solution.cpp");
        FileUtils.writeStringToFile(subFile, "code", StandardCharsets.UTF_8);

        submissionService.delete(123L);

        assertThat(subFile.exists()).isFalse();
        verify(submissionRepository).deleteById(123L);
    }

    @Test
    public void testBanAuthorSubmissions() throws IOException {
        Submission s1 = new Submission();
        s1.setId(1L);
        s1.setVerdict("wrong");
        s1.setPoints(0); // Should be skipped

        Submission s2 = new Submission();
        s2.setId(2L);
        s2.setVerdict("ok");
        s2.setPoints(100);
        // Will be skipped because files missing

        java.util.List<Submission> submissions = java.util.Arrays.asList(s1, s2);
        when(submissionRepository.findSubmissionsInPeriod(5, 1)).thenReturn(submissions);

        submissionService.banAuthorSubmissions();

        verify(submissionRepository, never()).save(s1);
        verify(submissionRepository, never()).save(s2);
    }
}
