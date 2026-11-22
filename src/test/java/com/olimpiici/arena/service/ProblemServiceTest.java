package com.olimpiici.arena.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
import com.olimpiici.arena.service.util.ProblemZipUtils;

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

    @Test
    public void testPopulateCompetitionInfo_MultipleCpsAndIdempotentWithLargeIds() {
        Competition root = new Competition();
        root.setId(1L);
        root.setLabel("Root");

        Competition yearComp = new Competition();
        yearComp.setId(500L);
        yearComp.setLabel("2025");
        yearComp.setParent(root);

        Competition comp1 = new Competition();
        comp1.setId(513L);
        comp1.setLabel("Comp One");
        comp1.setParent(yearComp);

        Competition comp2 = new Competition();
        comp2.setId(439L);
        comp2.setLabel("Comp Two");
        comp2.setParent(yearComp);

        Competition groupA = new Competition();
        groupA.setId(600L);
        groupA.setLabel("A");
        groupA.setParent(comp1);

        Competition groupT = new Competition();
        groupT.setId(601L);
        groupT.setLabel("T");
        groupT.setParent(comp2);

        Problem problem = new Problem();
        problem.setId(2256L);

        CompetitionProblem cp1 = new CompetitionProblem();
        cp1.setId(1000L);
        cp1.setCompetition(groupA);
        cp1.setProblem(problem);

        CompetitionProblem cp2 = new CompetitionProblem();
        cp2.setId(1001L);
        cp2.setCompetition(groupT);
        cp2.setProblem(problem);

        // Return CPs out of ID order to verify deterministic selection by highest CP ID
        when(competitionProblemRepository.findAll()).thenReturn(Arrays.asList(cp2, cp1));

        // First run: should save ONCE (not twice!) with deterministic final competition info (cp2 has higher ID)
        problemService.populateCompetitionInfo();

        assertThat(problem.getYear()).isEqualTo(2025);
        assertThat(problem.getGroup()).isEqualTo("T");
        assertThat(problem.getCompetition().getId()).isEqualTo(439L);
        assertThat(problem.getCanonicalCompetitionProblem().getId()).isEqualTo(1001L);
        verify(problemRepository).save(problem);

        // Simulate a fresh Hibernate session with distinct Long objects (>127, not ==)
        Competition freshComp2 = new Competition();
        freshComp2.setId(new Long(439L));
        CompetitionProblem freshCp2 = new CompetitionProblem();
        freshCp2.setId(new Long(1001L));
        problem.setCompetition(freshComp2);
        problem.setCanonicalCompetitionProblem(freshCp2);

        // Second run: nothing changed, so save must NOT be called again
        problemService.populateCompetitionInfo();
        verify(problemRepository).save(problem); // still only 1 total invocation
    }

    @Test
    public void testGetTaskDescription_MissingDirectoryReturnsEmpty() {
        Optional<File> result = problemService.getTaskDescription(workDir.getAbsolutePath(), 9999L);
        assertThat(result).isEmpty();
    }

    @Test
    public void testGetAuthorSolution_MissingDirectoryReturnsEmpty() {
        Optional<File> result = problemService.getAuthorSolution(workDir.getAbsolutePath(), 9999L);
        assertThat(result).isEmpty();
    }

    @Test
    public void testFormatTimeLimitSeconds() {
        assertThat(ProblemService.formatTimeLimitSeconds(1000)).isEqualTo("1");
        assertThat(ProblemService.formatTimeLimitSeconds(2000)).isEqualTo("2");
        assertThat(ProblemService.formatTimeLimitSeconds(1500)).isEqualTo("1.5");
        assertThat(ProblemService.formatTimeLimitSeconds(1050)).isEqualTo("1.05");
        assertThat(ProblemService.formatTimeLimitSeconds(1005)).isEqualTo("1.005");
        assertThat(ProblemService.formatTimeLimitSeconds(250)).isEqualTo("0.25");
        assertThat(ProblemService.formatTimeLimitSeconds(10)).isEqualTo("0.01");
    }

    @Test
    public void testUpdateLimits_SkipsRewriteWhenNumericallyEqual() throws Exception {
        File problemDir = new File(workDir, "problems/2/problem");
        problemDir.mkdirs();
        FileUtils.writeStringToFile(new File(problemDir, "grade.properties"), "time = 1.000 \nmemory = 512 \n", "UTF-8");

        // No problem.zip exists; if a rewrite were attempted, it would fail and/or call workerPool.deleteProblem
        problemService.updateTimeLimit(2L, 1000);
        problemService.updateMemoryLimit(2L, 512);

        verify(workerPool, never()).deleteProblem(anyLong());
    }

    @Test
    public void testUpdateLimits_RejectsNonPositiveValues() {
        assertThatThrownBy(() -> problemService.updateTimeLimit(2L, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> problemService.updateTimeLimit(2L, -100))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> problemService.updateMemoryLimit(2L, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> problemService.updateMemoryLimit(2L, -64))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSetLimitsToDto_TrimsAndNormalizesValues() throws IOException {
        File problemDir = new File(workDir, "problems/3/problem");
        problemDir.mkdirs();
        FileUtils.writeStringToFile(
            new File(problemDir, "grade.properties"),
            "time = 1.05 \nmemory = 512 \nextensions = ZIP , cpp \n",
            "UTF-8"
        );

        ProblemDTO dto = new ProblemDTO();
        dto.setId(3L);
        problemService.setLimitsToDto(dto);

        assertThat(dto.getTime()).isEqualTo(1050);
        assertThat(dto.getMemory()).isEqualTo(512);
        assertThat(dto.getExtension()).isEqualTo("zip");
        assertThat(problemService.getSolutionFileExtension(3L)).isEqualTo("zip");
    }

    @Test
    public void testSetLimitsToDto_FallsBackOnMalformedOrEmptyValues() throws IOException {
        File problemDir = new File(workDir, "problems/4/problem");
        problemDir.mkdirs();
        FileUtils.writeStringToFile(
            new File(problemDir, "grade.properties"),
            "time = \nmemory = 256MB\nextensions =   \n",
            "UTF-8"
        );

        ProblemDTO dto = new ProblemDTO();
        dto.setId(4L);
        problemService.setLimitsToDto(dto);

        assertThat(dto.getTime()).isEqualTo(1000);
        assertThat(dto.getMemory()).isEqualTo(256);
        assertThat(dto.getExtension()).isEqualTo("cpp");
        assertThat(problemService.getSolutionFileExtension(4L)).isEqualTo("cpp");
    }

    @Test
    public void testReplaceProblemZip_StagesAndPreservesPreviousPackageOnFailure() throws Exception {
        Map<String, String> valid = new LinkedHashMap<>();
        valid.put("grade.properties", "time=2\nmemory=512\n");
        valid.put("statement/task.pdf", "%PDF-1.4");
        byte[] validZipBytes = buildZipBytes(valid);

        problemService.replaceProblemZip(10L, new ByteArrayInputStream(validZipBytes));

        Properties props = problemService.getProperties(10L);
        assertThat(props.getProperty("time")).isEqualTo("2");
        assertThat(new File(workDir, "problems/10/problem/statement/task.pdf")).isFile();

        // Now try uploading a Zip-Slip archive: must fail AND preserve the previous working package
        Map<String, String> malicious = new LinkedHashMap<>();
        malicious.put("../escape.txt", "boom");
        byte[] evilZipBytes = buildZipBytes(malicious);

        assertThatThrownBy(() -> problemService.replaceProblemZip(10L, new ByteArrayInputStream(evilZipBytes)))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Zip-Slip");

        assertThat(problemService.getProperties(10L).getProperty("time")).isEqualTo("2");
        assertThat(new File(workDir, "problems/10/problem/statement/task.pdf")).isFile();
    }

    @Test
    public void testUpdateLimits_StreamUpdatesZipWithoutReExtractingDeletedTests() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("grade.properties", "time=1\nmemory=256\ngroups=1-2,3-4\nweights=50,50\n");
        entries.put("tests/p.01.in", "1 2\n");
        entries.put("tests/p.01.sol", "3\n");
        problemService.replaceProblemZip(11L, new ByteArrayInputStream(buildZipBytes(entries)));

        // Simulate Friday cleanup.sh deleting problem/tests/
        File testsDir = new File(workDir, "problems/11/problem/tests");
        FileUtils.deleteDirectory(testsDir);
        assertThat(testsDir).doesNotExist();

        problemService.updateTimeLimit(11L, 1050);
        problemService.updateMemoryLimit(11L, 512);

        // Tests must NOT be re-extracted onto disk, yet MUST still be intact in problem.zip
        assertThat(testsDir).doesNotExist();
        File zipFile = new File(workDir, "problems/11/problem.zip");
        assertThat(ProblemZipUtils.listFileEntries(zipFile))
            .containsExactlyInAnyOrder("grade.properties", "tests/p.01.in", "tests/p.01.sol");

        Properties updatedFromZip = new Properties();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(
                ProblemZipUtils.readEntry(zipFile, "grade.properties").get())) {
            updatedFromZip.load(bais);
        }
        assertThat(updatedFromZip.getProperty("time")).isEqualTo("1.05");
        assertThat(updatedFromZip.getProperty("memory")).isEqualTo("512");
        assertThat(updatedFromZip.getProperty("groups")).isEqualTo("1-2,3-4");
        assertThat(updatedFromZip.getProperty("weights")).isEqualTo("50,50");

        // On-disk copy in problem/ must also be updated
        assertThat(problemService.getProperties(11L).getProperty("time")).isEqualTo("1.05");
        assertThat(problemService.getProperties(11L).getProperty("memory")).isEqualTo("512");
        verify(workerPool, org.mockito.Mockito.times(2)).deleteProblem(11L);
    }

    @Test
    public void testWrappedFolderGradeProperties_ReadAndInPlaceUpdateWithoutDuplicate() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("A1_belote/grade.properties", "time=1.5\nmemory=1024\ngroups=1-1,2-8\nweights=0,100\nextensions=zip\n");
        entries.put("A1_belote/tests/belote.01.in", "1\n");
        problemService.replaceProblemZip(12L, new ByteArrayInputStream(buildZipBytes(entries)));

        ProblemDTO dto = new ProblemDTO();
        dto.setId(12L);
        problemService.setLimitsToDto(dto);
        assertThat(dto.getTime()).isEqualTo(1500);
        assertThat(dto.getMemory()).isEqualTo(1024);
        assertThat(dto.getExtension()).isEqualTo("zip");

        problemService.updateTimeLimit(12L, 2000);

        File zipFile = new File(workDir, "problems/12/problem.zip");
        // Must update A1_belote/grade.properties in place and NOT create a second root grade.properties
        assertThat(ProblemZipUtils.listFileEntries(zipFile))
            .containsExactlyInAnyOrder("A1_belote/grade.properties", "A1_belote/tests/belote.01.in");
        assertThat(new File(workDir, "problems/12/problem/grade.properties")).doesNotExist();

        Properties updated = problemService.getProperties(12L);
        assertThat(updated.getProperty("time")).isEqualTo("2");
        assertThat(updated.getProperty("memory")).isEqualTo("1024");
        assertThat(updated.getProperty("groups")).isEqualTo("1-1,2-8");
        assertThat(updated.getProperty("weights")).isEqualTo("0,100");
    }

    @Test
    public void testTestsSubdirGradeProperties_SurvivesCleanupShDeletion() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("tests/grade.properties", "time=0.1\nmemory=512\ngroups=1-5\n");
        entries.put("tests/cowboys.01.in", "1\n");
        entries.put("statement/cowboys.pdf", "%PDF-1.4");
        problemService.replaceProblemZip(13L, new ByteArrayInputStream(buildZipBytes(entries)));

        // Simulate Friday cleanup.sh deleting problem/tests/ (including tests/grade.properties!)
        File testsDir = new File(workDir, "problems/13/problem/tests");
        FileUtils.deleteDirectory(testsDir);
        assertThat(testsDir).doesNotExist();

        // Reading limits must still fall back to problem.zip
        ProblemDTO dto = new ProblemDTO();
        dto.setId(13L);
        problemService.setLimitsToDto(dto);
        assertThat(dto.getTime()).isEqualTo(100);
        assertThat(dto.getMemory()).isEqualTo(512);

        // Editing time limit must update tests/grade.properties inside problem.zip preserving groups,
        // without creating a duplicate root grade.properties or recreating problem/tests/
        problemService.updateTimeLimit(13L, 250);
        assertThat(testsDir).doesNotExist();
        assertThat(new File(workDir, "problems/13/problem/grade.properties")).doesNotExist();

        File zipFile = new File(workDir, "problems/13/problem.zip");
        assertThat(ProblemZipUtils.listFileEntries(zipFile))
            .containsExactlyInAnyOrder("tests/grade.properties", "tests/cowboys.01.in", "statement/cowboys.pdf");

        Properties updated = problemService.getProperties(13L);
        assertThat(updated.getProperty("time")).isEqualTo("0.25");
        assertThat(updated.getProperty("memory")).isEqualTo("512");
        assertThat(updated.getProperty("groups")).isEqualTo("1-5");
    }

    @Test
    public void testGetProperties_PrefersAuthoritativeZipOverStaleUnzippedCopy() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("grade.properties", "time=2.5\nmemory=512\n");
        problemService.replaceProblemZip(14L, new ByteArrayInputStream(buildZipBytes(entries)));

        // Mutate only the unzipped copy to simulate drift
        FileUtils.writeStringToFile(
            new File(workDir, "problems/14/problem/grade.properties"),
            "time=9\nmemory=64\n",
            "UTF-8"
        );

        Properties props = problemService.getProperties(14L);
        assertThat(props.getProperty("time")).isEqualTo("2.5");
        assertThat(props.getProperty("memory")).isEqualTo("512");
    }

    private byte[] buildZipBytes(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
