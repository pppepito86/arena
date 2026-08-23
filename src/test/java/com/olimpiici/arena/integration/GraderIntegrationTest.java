package com.olimpiici.arena.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olimpiici.arena.ArenaApp;
import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.grader.GraderTask;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.CompetitionRepository;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.util.RandomUtil;

/**
 * Runs the full arena -> worker -> grader -> isolate pipeline for every
 * "problem" found under src/test/resources/integration-test/.
 *
 * Each problem directory must contain:
 *  - problem/    the problem package (zipped and served to the worker as-is)
 *  - solutions/  one source file per case (e.g. correct.cpp), each paired
 *                with a same-named ".json" file describing the expected
 *                verdict/points (e.g. correct.json). See
 *                {@link SolutionExpectation} for the supported fields.
 *
 * Add a new problem by dropping a new directory under integration-test/;
 * add a new case by dropping a solution + expectation file pair into an
 * existing problem's solutions/ directory. No test code changes needed.
 */
@RunWith(Parameterized.class)
@SpringBootTest(classes = ArenaApp.class, properties = {
    "application.workDir=target/integration-test-work",
    "application.workerUrl=localhost:8089",
    // The test drives grading itself via graderTask.gradeTask(); the real
    // background @Scheduled job must stay off, otherwise it independently
    // grades "waiting" submissions every 3s and races the explicit calls,
    // colliding on the worker's shared isolate sandbox box.
    "application.autoGradeEnabled=false"
})
@ActiveProfiles("test")
public class GraderIntegrationTest {

    private static final File INTEGRATION_TEST_ROOT = new File("src/test/resources/integration-test");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static Process workerProcess;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CompetitionProblemRepository competitionProblemRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private GraderTask graderTask;

    @Autowired
    private ApplicationProperties applicationProperties;

    private User testUser;
    private Problem testProblem;
    private CompetitionProblem testCompProblem;

    private final File problemDir;
    private final File solutionFile;
    private final SolutionExpectation expectation;

    public GraderIntegrationTest(String caseName, File problemDir, File solutionFile, SolutionExpectation expectation) {
        this.problemDir = problemDir;
        this.solutionFile = solutionFile;
        this.expectation = expectation;
    }

    @Parameters(name = "{0}")
    public static List<Object[]> data() throws IOException {
        List<Object[]> params = new ArrayList<>();

        File[] problemRoots = INTEGRATION_TEST_ROOT.listFiles(File::isDirectory);
        if (problemRoots == null) {
            throw new IllegalStateException("No integration-test problems found under " + INTEGRATION_TEST_ROOT);
        }
        Arrays.sort(problemRoots, Comparator.comparing(File::getName));

        for (File problemRoot : problemRoots) {
            File problemDir = new File(problemRoot, "problem");
            File solutionsDir = new File(problemRoot, "solutions");
            if (!problemDir.isDirectory() || !solutionsDir.isDirectory()) {
                continue;
            }

            File[] solutionFiles = solutionsDir.listFiles((dir, name) -> !name.endsWith(".json"));
            Arrays.sort(solutionFiles, Comparator.comparing(File::getName));

            for (File solutionFile : solutionFiles) {
                String baseName = FilenameUtils.getBaseName(solutionFile.getName());
                File expectationFile = new File(solutionsDir, baseName + ".json");
                if (!expectationFile.isFile()) {
                    throw new IllegalStateException("Missing expectation file for solution "
                        + solutionFile + ": expected " + expectationFile);
                }

                SolutionExpectation expectation = MAPPER.readValue(expectationFile, SolutionExpectation.class);
                String caseName = problemRoot.getName() + "/" + baseName;
                params.add(new Object[] { caseName, problemDir, solutionFile, expectation });
            }
        }

        if (params.isEmpty()) {
            throw new IllegalStateException("No solutions found under " + INTEGRATION_TEST_ROOT);
        }
        return params;
    }

    @BeforeClass
    public static void startWorker() throws Exception {
        // Kill anything on port 8089 first
        killPort(8089);

        // Reset isolate sandbox to ensure compatible cgroup mode
        try {
            new ProcessBuilder("isolate", "--cleanup").start().waitFor();
        } catch (Exception e) {
            System.out.println("isolate --cleanup failed (may be ok): " + e.getMessage());
        }
        try {
            new ProcessBuilder("isolate", "--cg", "--init").start().waitFor();
        } catch (Exception e) {
            System.out.println("isolate --cg --init failed: " + e.getMessage());
        }

        File workerDir = new File("../worker");

        // Wipe the worker's problem/submission cache before every run so stale
        // data from a previous test invocation can't collide with this run's
        // Problem ids (which restart from 1 in the fresh in-memory arena DB).
        File workerWorkDir = new File(workerDir, "target/work");
        FileUtils.deleteDirectory(workerWorkDir);

        // NOTE: worker/pom.xml pins spring-boot-maven-plugin to 1.5.7.RELEASE
        // (via spring-boot-starter-parent), whose `run` goal reads program
        // arguments from the "run.arguments" system property, COMMA-separated
        // -- not "spring-boot.run.arguments" space-separated (that property
        // name is Spring Boot 2.x only). Using the wrong one is silently
        // ignored, so the worker falls back to application.properties's
        // work.dir=./work -- a directory shared and never cleaned across
        // every local run of this test/service.
        ProcessBuilder pb = new ProcessBuilder("./mvnw", "spring-boot:run",
            "-Drun.arguments=--server.port=8089,--work.dir=target/work");
        pb.directory(workerDir);
        pb.redirectErrorStream(true);
        File logFile = new File("target/worker-integration.log");
        logFile.getParentFile().mkdirs();
        pb.redirectOutput(ProcessBuilder.Redirect.to(logFile));

        System.out.println("Starting worker process...");
        workerProcess = pb.start();

        // Poll healthcheck until worker is up (max 45s)
        long start = System.currentTimeMillis();
        boolean workerUp = false;
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet("http://localhost:8089/api/v1/health-check");
        while (System.currentTimeMillis() - start < 120000) {
            try {
                HttpResponse response = client.execute(request);
                if (response.getStatusLine().getStatusCode() == 200) {
                    workerUp = true;
                    break;
                }
            } catch (Exception e) {
                // Ignore and wait
            }
            Thread.sleep(5000);
        }

        if (!workerUp) {
            stopWorker();
            throw new IllegalStateException("Worker failed to start on port 8089 within 45 seconds");
        }
        System.out.println("Worker started successfully!");
    }

    @AfterClass
    public static void stopWorker() {
        if (workerProcess != null) {
            System.out.println("Stopping worker process...");
            workerProcess.destroy();
            try {
                workerProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        killPort(8089);
    }

    private static void killPort(int port) {
        try {
            new ProcessBuilder("fuser", "-k", port + "/tcp").start().waitFor();
        } catch (Exception e) {
            // Ignore
        }
    }

    @Before
    public void setup() throws Exception {
        // Clean up in dependency order: submissions reference competition_problem
        submissionRepository.deleteAll();
        competitionProblemRepository.deleteAll();
        problemRepository.deleteAll();
        competitionRepository.deleteAll();

        // Ensure user exists
        testUser = userRepository.findOneByLogin("user").orElseGet(() -> {
            User user = new User();
            user.setLogin("user");
            user.setPassword("passwordpasswordpasswordpasswordpasswordpasswordpasswordpass");
            user.setActivated(true);
            return userRepository.saveAndFlush(user);
        });

        // Create problem
        testProblem = new Problem();
        testProblem.setTitle("A+B Problem");
        testProblem = problemRepository.saveAndFlush(testProblem);

        // Create competition
        Competition comp = new Competition();
        comp.setLabel("Integration Test Competition");
        comp = competitionRepository.saveAndFlush(comp);

        // Link them
        testCompProblem = new CompetitionProblem();
        testCompProblem.setCompetition(comp);
        testCompProblem.setProblem(testProblem);
        testCompProblem = competitionProblemRepository.saveAndFlush(testCompProblem);

        // Create the problem.zip dynamically in the arena workdir
        File problemWorkDir = new File(applicationProperties.getWorkDir(), "problems/" + testProblem.getId());
        problemWorkDir.mkdirs();
        File zipFile = new File(problemWorkDir, "problem.zip");

        zipDirectory(problemDir, zipFile);
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    public void testSubmitSolution() throws Exception {
        SubmissionDTO submission = submitSolution(solutionFile);

        // Trigger grader task
        graderTask.gradeTask();

        // Poll for verdict
        SubmissionDTO result = waitForFinalVerdict(submission.getId());

        if (expectation.getVerdict() != null) {
            assertThat(result.getVerdict())
                .as("verdict for %s", solutionFile)
                .isEqualTo(expectation.getVerdict());
        }
        if (expectation.getVerdictContains() != null) {
            assertThat(result.getVerdict())
                .as("verdict for %s", solutionFile)
                .contains(expectation.getVerdictContains());
        }
        if (expectation.getPoints() != null) {
            assertThat(result.getPoints())
                .as("points for %s", solutionFile)
                .isEqualTo(expectation.getPoints());
        }
        if (expectation.getPointsLessThan() != null) {
            assertThat(result.getPoints())
                .as("points for %s", solutionFile)
                .isLessThan(expectation.getPointsLessThan());
        }
    }

    private SubmissionDTO submitSolution(File srcFile) throws Exception {
        // Create Submission DTO
        SubmissionDTO submission = new SubmissionDTO();
        submission.setUserId(testUser.getId());
        submission.setCompetitionProblemId(testCompProblem.getId());
        submission.setUploadDate(ZonedDateTime.now());
        submission.setSecurityKey(RandomUtil.generateSubmissionSecurityKey());
        submission = submissionService.save(submission);

        // Copy source code to submission directory
        File submissionDir = new File(applicationProperties.getWorkDir(), "submissions/" + submission.getId());
        submissionDir.mkdirs();
        String extension = FilenameUtils.getExtension(srcFile.getName());
        File submissionFile = new File(submissionDir, "solution." + extension);
        FileUtils.copyFile(srcFile, submissionFile);

        // Set verdict to waiting and save
        submission.setVerdict("waiting");
        return submissionService.save(submission);
    }

    private SubmissionDTO waitForFinalVerdict(long submissionId) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 60000) {
            Optional<SubmissionDTO> sub = submissionService.findOne(submissionId);
            if (sub.isPresent()) {
                String verdict = sub.get().getVerdict();
                if (verdict != null && !verdict.equals("waiting") && !verdict.equals("judging")) {
                    return sub.get();
                }
            }
            Thread.sleep(1000);
        }
        throw new AssertionError("Submission did not finish grading within 30 seconds");
    }

    private static void zipDirectory(File source, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zip(source, source, zos);
        }
    }

    private static void zip(File root, File file, ZipOutputStream zos) throws IOException {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                zip(root, child, zos);
            }
        } else {
            String relativePath = root.toURI().relativize(file.toURI()).getPath();
            zos.putNextEntry(new ZipEntry(relativePath));
            Files.copy(file.toPath(), zos);
            zos.closeEntry();
        }
    }
}
