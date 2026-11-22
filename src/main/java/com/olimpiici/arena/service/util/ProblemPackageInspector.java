package com.olimpiici.arena.service.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.pesho.grader.task.TaskDetails;
import org.pesho.grader.task.TestCase;

import com.olimpiici.arena.service.ProblemService;
import com.olimpiici.arena.service.dto.ProblemPackageInfoDTO;

/**
 * Inspects a problem package (problem.zip and/or unzipped problem/ directory) to produce
 * an admin-facing {@link ProblemPackageInfoDTO} summary without leaking file descriptors
 * or crashing on malformed properties.
 */
public final class ProblemPackageInspector {

    private static final int DEFAULT_TIME_MS = 1000;
    private static final int DEFAULT_MEMORY_MB = 256;

    /**
     * Files whose content the grader's {@link TaskDetails} reads (PropertiesFinder,
     * CriteriaFinder, QuizFinder); all other files are only looked at by name.
     */
    private static final Set<String> GRADER_CONTENT_FILES = new HashSet<>(Arrays.asList(
        "grade.properties", "grade.txt", "task.properties", "task.txt",
        "criteria.json", "quiz.json", "task.json"));

    private ProblemPackageInspector() {
    }

    public static ProblemPackageInfoDTO inspect(File zipFile, File unzippedDir, String problemTitle) throws IOException {
        ProblemPackageInfoDTO info = new ProblemPackageInfoDTO();

        boolean hasZip = zipFile != null && zipFile.isFile();
        boolean hasDir = unzippedDir != null && unzippedDir.isDirectory();
        if (!hasZip && !hasDir) {
            info.setHasPackage(false);
            return info;
        }

        List<String> diskEntries = hasDir ? ProblemPackageResolver.listRelativeFiles(unzippedDir) : Collections.emptyList();
        List<String> zipEntries = Collections.emptyList();
        Map<String, byte[]> zipContents = new HashMap<>();
        List<String> allProps = Collections.emptyList();
        boolean zipReadable = false;

        if (hasZip) {
            try (ProblemZipUtils.OpenedZip opened = ProblemZipUtils.openZipFile(zipFile)) {
                zipEntries = ProblemZipUtils.listFileEntries(opened);
                for (String entry : zipEntries) {
                    if (GRADER_CONTENT_FILES.contains(ProblemPackageResolver.fileName(entry).toLowerCase(Locale.ROOT))) {
                        Optional<byte[]> bytes = ProblemZipUtils.readEntry(opened, entry);
                        if (bytes.isPresent()) {
                            zipContents.put(entry, bytes.get());
                        }
                    }
                }
                zipReadable = true;
                allProps = ProblemPackageResolver.findAllPropertiesEntries(zipEntries);
            } catch (IOException e) {
                info.addWarning("problem.zip could not be read: " + e.getMessage());
            }
        }

        List<String> entries = zipReadable ? zipEntries : diskEntries;
        if (!zipReadable) {
            allProps = ProblemPackageResolver.findAllPropertiesEntries(entries);
        }

        if (entries.isEmpty()) {
            info.setHasPackage(false);
            if (info.getWarnings().isEmpty()) {
                info.addWarning("Uploaded archive is empty.");
            }
            return info;
        }

        info.setHasPackage(true);

        // 1. Properties discovery & validation
        Optional<String> propsEntry = allProps.isEmpty() ? Optional.empty() : Optional.of(allProps.get(0));
        Properties props = new Properties();

        if (allProps.size() > 1) {
            info.addWarning("Multiple properties files found (" + String.join(", ", allProps)
                + "); the worker expects a single properties file and may ignore all of them.");
        }

        if (propsEntry.isPresent()) {
            info.setPropertiesPath(ProblemPackageResolver.normalizeRelativePath(propsEntry.get()));
            loadProperties(Optional.ofNullable(zipContents.get(propsEntry.get())), unzippedDir, propsEntry.get(), props);
        } else {
            info.addWarning("No grade.properties found; using default limits (1s, 256MB).");
        }

        String rawTime = props.getProperty("time");
        Optional<Integer> parsedTime = ProblemService.parseTimeMillis(rawTime);
        if (rawTime != null && !parsedTime.isPresent()) {
            info.addWarning("Invalid time limit '" + rawTime.trim() + "' in grade.properties; using default 1s.");
        } else if (rawTime != null && !rawTime.equals(rawTime.trim())) {
            info.addWarning("Time limit in grade.properties has extra whitespace ('" + rawTime
                + "'); save limits to clean it for the worker.");
        }
        info.setTimeMs(parsedTime.orElse(DEFAULT_TIME_MS));

        String rawMemory = props.getProperty("memory");
        Optional<Integer> parsedMemory = ProblemService.parseMemoryMb(rawMemory);
        if (rawMemory != null && !parsedMemory.isPresent()) {
            info.addWarning("Invalid memory limit '" + rawMemory.trim() + "' in grade.properties; using default 256MB.");
        } else if (rawMemory != null && !rawMemory.equals(rawMemory.trim())) {
            info.addWarning("Memory limit in grade.properties has extra whitespace ('" + rawMemory
                + "'); save limits to clean it for the worker.");
        }
        info.setMemoryMb(parsedMemory.orElse(DEFAULT_MEMORY_MB));

        List<String> allowedExtensions = ProblemService.parseAllowedExtensions(props.getProperty("extensions"));
        info.setAllowedExtensions(allowedExtensions);
        info.setExtension(allowedExtensions.get(0));

        // 2. Statement PDF & author solution
        Optional<String> statement = ProblemPackageResolver.findStatementPdf(entries);
        if (statement.isPresent()) {
            info.setStatementPath(ProblemPackageResolver.normalizeRelativePath(statement.get()));
        } else {
            info.addWarning("No statement PDF found in archive.");
        }

        Optional<String> authorSolution = ProblemPackageResolver.findAuthorSolution(entries, allowedExtensions, problemTitle);
        if (authorSolution.isPresent()) {
            info.setAuthorSolutionPath(ProblemPackageResolver.normalizeRelativePath(authorSolution.get()));
        } else {
            info.addWarning("No author solution detected; auto-submission will be skipped.");
        }

        // 3. Worker view: run the grader's own TaskDetails parser, exactly as the worker does.
        //    From problem.zip a stub tree is used, so the result does not depend on whether
        //    cleanup.sh has deleted problem/tests/ yet.
        TaskDetails task;
        Path taskRoot = null;
        try {
            if (zipReadable) {
                taskRoot = Files.createTempDirectory("arena-pkg-");
                writeStubTree(taskRoot, zipEntries, zipContents);
            } else {
                taskRoot = unzippedDir.toPath();
            }
            task = new TaskDetails("task", taskRoot);
        } catch (IOException | RuntimeException e) {
            info.addWarning("Could not inspect worker configuration: " + e.getMessage());
            return info;
        } finally {
            if (zipReadable && taskRoot != null) {
                FileUtils.deleteQuietly(taskRoot.toFile());
            }
        }
        List<String> inputs = applyWorkerView(task, taskRoot, entries, props, info);
        if (zipReadable && hasDir) {
            info.setTestsCleanedUp(inputs.stream().anyMatch(in -> !new File(unzippedDir, in).isFile()));
        }

        return info;
    }

    private static void loadProperties(Optional<byte[]> zipPropsBytes, File unzippedDir, String entryPath, Properties props) throws IOException {
        if (zipPropsBytes.isPresent()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(zipPropsBytes.get())) {
                props.load(bais);
                return;
            }
        }
        if (unzippedDir == null) {
            return;
        }
        File onDisk = new File(unzippedDir, ProblemPackageResolver.normalizeRelativePath(entryPath));
        if (onDisk.isFile()) {
            try (FileInputStream fis = new FileInputStream(onDisk)) {
                props.load(fis);
            }
        }
    }

    /**
     * Mirrors the package as a tree of empty files under {@code root}, so the grader's
     * {@link TaskDetails} can parse it. The grader only reads the content of the files listed in
     * {@link #GRADER_CONTENT_FILES}; everything else (tests, checker, grader, manager) is
     * detected from paths alone, so no test data has to be extracted. The caller creates
     * {@code root} with {@link Files#createTempDirectory}, so concurrent inspections never share it.
     */
    private static void writeStubTree(Path root, List<String> entries, Map<String, byte[]> contents) throws IOException {
        for (String entry : entries) {
            if (ProblemPackageResolver.isIgnoredPath(entry)) {
                continue;
            }
            Path target = root.resolve(ProblemPackageResolver.normalizeRelativePath(entry)).normalize();
            if (!target.startsWith(root) || target.equals(root) || Files.exists(target)) {
                continue;
            }
            try {
                Files.createDirectories(target.getParent());
                byte[] content = contents.get(entry);
                if (content != null) {
                    Files.write(target, content);
                } else {
                    Files.createFile(target);
                }
            } catch (IOException e) {
                // A file and a directory with the same name; extraction would fail on it too.
            }
        }
    }

    /**
     * Copies the worker's view into the DTO. {@link TaskDetails} returns paths resolved against
     * the task root (and the compiled binary path for .cpp checkers/managers), so they are made
     * relative again here.
     *
     * @return the relative paths of the detected test inputs
     */
    private static List<String> applyWorkerView(TaskDetails task, Path root, List<String> entries, Properties props, ProblemPackageInfoDTO info) {
        info.setCheckerPath(relativize(root, task.getCppChecker() != null ? task.getCppChecker() : task.getChecker()));
        info.setGraderPath(graderSourcePath(relativize(root, task.getGraderDir()), entries));
        info.setManagerPath(relativize(root, task.getCppManager() != null ? task.getCppManager() : task.getManager()));

        List<String> inputs = task.getTestGroups().stream()
            .flatMap(group -> group.getTestCases().stream())
            .map(TestCase::getInput)
            .filter(Objects::nonNull)
            .map(input -> relativize(root, input))
            .distinct()
            .collect(Collectors.toList());
        info.setTestCount(inputs.size());
        // Without groups/patterns the worker treats every test as its own group; report 0 then.
        boolean groupsConfigured = !props.getProperty("groups", "").trim().isEmpty()
            || !props.getProperty("patterns", "").trim().isEmpty();
        info.setGroupCount(groupsConfigured ? task.getTestGroups().size() : 0);

        task.getError().forEach((key, value) -> humanizeWorkerError(key, value).ifPresent(info::addWarning));
        if (inputs.isEmpty() && !task.getError().containsKey("tests_parse")) {
            info.addWarning("No test cases detected in archive.");
        }
        return inputs;
    }

    /**
     * The worker finds the interactive grader by its file name (grader.cpp, any case) and hands
     * its directory to the compile step. Show the grader.cpp itself when it sits directly in that
     * directory. With several allowed extensions the worker instead uses the parent of the
     * per-language grader directories, so the directory is shown then.
     */
    private static String graderSourcePath(String graderDir, List<String> entries) {
        if (graderDir == null) {
            return null;
        }
        String prefix = graderDir + "/";
        return entries.stream()
            .map(ProblemPackageResolver::normalizeRelativePath)
            .filter(e -> e.startsWith(prefix) && e.substring(prefix.length()).equalsIgnoreCase("grader.cpp"))
            .findFirst()
            .orElse(graderDir);
    }

    private static String relativize(Path root, String path) {
        if (path == null) {
            return null;
        }
        Path p = Paths.get(path);
        return ProblemPackageResolver.toForwardSlash((p.startsWith(root) ? root.relativize(p) : p).toString());
    }

    private static Optional<String> humanizeWorkerError(String key, String value) {
        switch (key) {
            case "tests_parse":
                return Optional.of(humanizeTestParseError(value));
            case "statement":
                // Arena resolves statements itself (see findStatementPdf above).
            case "time_property":
            case "memory_property":
                // Already reported with a clearer message by the limit checks above.
                return Optional.empty();
            case "exception":
                return Optional.of("The worker cannot parse this package and will ignore grade.properties: " + value);
            default:
                String detail = value == null || value.isEmpty() ? "" : " (" + value.replace("backend.", "").replace('_', ' ') + ")";
                if (key.endsWith("_property")) {
                    return Optional.of("Invalid '" + key.substring(0, key.length() - "_property".length())
                        + "' in grade.properties" + detail + ".");
                }
                return Optional.of("Worker reported a problem with '" + key + "'" + detail + ".");
        }
    }

    private static String humanizeTestParseError(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "Could not detect test cases in archive.";
        }
        if ("backend.no_patterns".equals(raw)) {
            return "No numbered test case files detected in archive.";
        }
        if ("backend.no_tests_folder".equals(raw)) {
            return "grade.properties specifies 'patterns', but no tests/ directory was found.";
        }
        if (raw.startsWith("backend.patterns:")) {
            return "Could not pair input/output test files automatically (check test filenames or add a checker).";
        }
        return "Could not detect test cases: " + raw;
    }
}
