package com.olimpiici.arena.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.olimpiici.arena.service.dto.ProblemPackageInfoDTO;

public class ProblemPackageInspectorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testInspect_FullInfosInteractiveArchive() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("mummy/grade.properties", "time=3\nmemory=1024\ngroups=1-1,2-3\nweights=30,70\n");
        entries.put("mummy/checker/checker.cpp", "int main(){}");
        entries.put("mummy/system/grader.cpp", "int main(){}");
        entries.put("mummy/contestant/grader.cpp", "int main(){}");
        entries.put("mummy/contestant/mummy.cpp", "// stub");
        entries.put("mummy/solutions/mummy_100p.cpp", "// full");
        entries.put("mummy/statement/mummy.pdf", "%PDF-1.4");
        entries.put("mummy/analysis/mummy_analysis.pdf", "%PDF-analysis");
        entries.put("mummy/tests/mummy.01.in", "1\n");
        entries.put("mummy/tests/mummy.01.sol", "1\n");
        entries.put("mummy/tests/mummy.02.in", "2\n");
        entries.put("mummy/tests/mummy.02.sol", "2\n");
        entries.put("mummy/tests/mummy.03.in", "3\n");
        entries.put("mummy/tests/mummy.03.sol", "3\n");

        File zipFile = createZip("mummy.zip", entries);
        File unzipped = folder.newFolder("mummy-unzipped");
        ProblemZipUtils.extractSafely(zipFile, unzipped);

        ProblemPackageInfoDTO info = ProblemPackageInspector.inspect(zipFile, unzipped, "mummy");

        assertThat(info.isHasPackage()).isTrue();
        assertThat(info.getPropertiesPath()).isEqualTo("mummy/grade.properties");
        assertThat(info.getTimeMs()).isEqualTo(3000);
        assertThat(info.getMemoryMb()).isEqualTo(1024);
        assertThat(info.getExtension()).isEqualTo("cpp");
        assertThat(info.getStatementPath()).isEqualTo("mummy/statement/mummy.pdf");
        assertThat(info.getAuthorSolutionPath()).isEqualTo("mummy/solutions/mummy_100p.cpp");
        assertThat(info.getCheckerPath()).isEqualTo("mummy/checker/checker.cpp");
        assertThat(info.getGraderPath()).isEqualTo("mummy/system/grader.cpp");
        assertThat(info.getManagerPath()).isNull();
        assertThat(info.getTestCount()).isEqualTo(3);
        assertThat(info.getGroupCount()).isEqualTo(2);
        assertThat(info.isTestsCleanedUp()).isFalse();
        assertThat(info.getWarnings()).isEmpty();
    }

    @Test
    public void testInspect_AfterCleanupShDeletesTestsDir() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("tests/grade.properties", "time=0.2\nmemory=512\n");
        entries.put("tests/cowboys.01.in", "1\n");
        entries.put("tests/cowboys.01.sol", "1\n");
        entries.put("tests/cowboys.02.in", "2\n");
        entries.put("tests/cowboys.02.sol", "2\n");
        entries.put("statement/cowboys.pdf", "%PDF-1.4");
        entries.put("author/author.cpp", "int main(){}");

        File zipFile = createZip("cowboys.zip", entries);
        File unzipped = folder.newFolder("cowboys-unzipped");
        ProblemZipUtils.extractSafely(zipFile, unzipped);
        FileUtils.deleteDirectory(new File(unzipped, "tests"));

        ProblemPackageInfoDTO info = ProblemPackageInspector.inspect(zipFile, unzipped, "cowboys");

        assertThat(info.isHasPackage()).isTrue();
        assertThat(info.isTestsCleanedUp()).isTrue();
        assertThat(info.getPropertiesPath()).isEqualTo("tests/grade.properties");
        assertThat(info.getTimeMs()).isEqualTo(200);
        assertThat(info.getMemoryMb()).isEqualTo(512);
        assertThat(info.getTestCount()).isEqualTo(2);
        assertThat(info.getWarnings()).isEmpty();
    }

    @Test
    public void testInspect_ReportsDuplicatePropertiesAndMissingFilesWithHumanReadableWarnings() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("grade.properties", "time=1\n");
        entries.put("sub/grade.properties", "time=2\n");
        entries.put("readme.txt", "hello");

        File zipFile = createZip("broken.zip", entries);
        File unzipped = folder.newFolder("broken-unzipped");
        ProblemZipUtils.extractSafely(zipFile, unzipped);

        ProblemPackageInfoDTO info = ProblemPackageInspector.inspect(zipFile, unzipped, "broken");

        assertThat(info.isHasPackage()).isTrue();
        assertThat(info.getTestCount()).isEqualTo(0);
        assertThat(info.getWarnings()).anyMatch(w -> w.contains("Multiple properties files"));
        assertThat(info.getWarnings()).anyMatch(w -> w.contains("No statement PDF"));
        assertThat(info.getWarnings()).anyMatch(w -> w.contains("No author solution"));
        assertThat(info.getWarnings()).noneMatch(w -> w.contains("backend."));
    }

    @Test
    public void testInspect_PatternsPropertyUsesV4Finder() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("grade.properties", "time=1.5\nmemory=1024\npatterns=madcows0,madcows1\nweights=40,60\n");
        entries.put("statement/madcows.pdf", "%PDF");
        entries.put("author/author.cpp", "int main(){}");
        entries.put("tests/madcows0.01.in", "1");
        entries.put("tests/madcows0.01.out", "1");
        entries.put("tests/madcows1.01.in", "2");
        entries.put("tests/madcows1.01.out", "2");

        File zipFile = createZip("madcows.zip", entries);
        File unzipped = folder.newFolder("madcows-unzipped");
        ProblemZipUtils.extractSafely(zipFile, unzipped);

        ProblemPackageInfoDTO info = ProblemPackageInspector.inspect(zipFile, unzipped, "madcows");

        assertThat(info.getTestCount()).isEqualTo(2);
        assertThat(info.getGroupCount()).isEqualTo(2);
        assertThat(info.getWarnings()).isEmpty();
    }

    @Test
    public void testInspect_CorruptZipReturnsWarningWithoutThrowing() throws IOException {
        File corruptZip = folder.newFile("corrupt.zip");
        FileUtils.writeStringToFile(corruptZip, "not-a-valid-zip", StandardCharsets.UTF_8);

        ProblemPackageInfoDTO info = ProblemPackageInspector.inspect(corruptZip, null, "corrupt");

        assertThat(info.isHasPackage()).isFalse();
        assertThat(info.getWarnings()).anyMatch(w -> w.contains("problem.zip could not be read"));
    }

    @Test
    public void testInspect_WarnsOnTrailingWhitespaceAndIgnoresRootGrader() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("grade.properties", "time=1 \nmemory=512 \n");
        entries.put("grader.cpp", "// root grader is not interactive in TaskDetails");
        entries.put("statement/task.pdf", "%PDF");
        entries.put("author/author.cpp", "int main(){}");
        entries.put("tests/p.01.in", "1\n");
        entries.put("tests/p.01.sol", "1\n");

        File zipFile = createZip("ws.zip", entries);
        File unzipped = folder.newFolder("ws-unzipped");
        ProblemZipUtils.extractSafely(zipFile, unzipped);

        ProblemPackageInfoDTO info = ProblemPackageInspector.inspect(zipFile, unzipped, "ws");

        assertThat(info.getGraderPath()).isNull();
        assertThat(info.getWarnings()).anyMatch(w -> w.contains("Time limit") && w.contains("extra whitespace"));
        assertThat(info.getWarnings()).anyMatch(w -> w.contains("Memory limit") && w.contains("extra whitespace"));
    }

    @Test
    public void testInspect_DuplicateNormalizedZipEntriesDoNotTriggerTestsCleanedUp() throws IOException {
        File zipFile = folder.newFile("dup.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            writeZipEntry(zos, "grade.properties", "time=1\nmemory=256\n");
            writeZipEntry(zos, "statement/task.pdf", "%PDF");
            writeZipEntry(zos, "author/author.cpp", "int main(){}");
            writeZipEntry(zos, "tests/p.01.in", "1\n");
            writeZipEntry(zos, "./tests/p.01.in", "1\n");
            writeZipEntry(zos, "tests/p.01.sol", "1\n");
            writeZipEntry(zos, "__MACOSX/._grade.properties", "mac");
        }
        File unzipped = folder.newFolder("dup-unzipped");
        ProblemZipUtils.extractSafely(zipFile, unzipped);

        ProblemPackageInfoDTO info = ProblemPackageInspector.inspect(zipFile, unzipped, "dup");

        assertThat(info.isTestsCleanedUp()).isFalse();
        assertThat(info.getTestCount()).isEqualTo(1);
    }

    @Test
    public void testInspect_InMemoryUnpairedTestsWithoutCheckerReportsZeroAndWarning() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("grade.properties", "time=1\nmemory=256\n");
        entries.put("statement/task.pdf", "%PDF");
        entries.put("author/author.cpp", "int main(){}");
        entries.put("tests/p.01.in", "1\n");
        entries.put("tests/p.02.in", "2\n");

        File zipFile = createZip("unpaired.zip", entries);
        ProblemPackageInfoDTO info = ProblemPackageInspector.inspect(zipFile, null, "unpaired");

        assertThat(info.getTestCount()).isEqualTo(0);
        assertThat(info.getWarnings()).anyMatch(w -> w.contains("pair input/output"));
    }

    @Test
    public void testInspect_PatternsWithTestsFromZeroMatchWorkerBeforeAndAfterCleanup() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("grade.properties", "time=1\nmemory=256\npatterns=tsk\ngroups=0,1-2\n");
        entries.put("statement/tsk.pdf", "%PDF");
        entries.put("author/author.cpp", "int main(){}");
        entries.put("tests/tsk.00.in", "0");
        entries.put("tests/tsk.00.out", "0");
        entries.put("tests/tsk.01.in", "1");
        entries.put("tests/tsk.01.out", "1");
        entries.put("tests/tsk.02.in", "2");
        entries.put("tests/tsk.02.out", "2");

        File zipFile = createZip("zero.zip", entries);
        File unzipped = folder.newFolder("zero-unzipped");
        ProblemZipUtils.extractSafely(zipFile, unzipped);

        ProblemPackageInfoDTO before = ProblemPackageInspector.inspect(zipFile, unzipped, "tsk");
        FileUtils.deleteDirectory(new File(unzipped, "tests"));
        ProblemPackageInfoDTO after = ProblemPackageInspector.inspect(zipFile, unzipped, "tsk");

        for (ProblemPackageInfoDTO info : new ProblemPackageInfoDTO[] { before, after }) {
            assertThat(info.getTestCount()).isEqualTo(3);
            assertThat(info.getGroupCount()).isEqualTo(2);
            assertThat(info.getWarnings()).isEmpty();
        }
        assertThat(before.isTestsCleanedUp()).isFalse();
        assertThat(after.isTestsCleanedUp()).isTrue();
    }

    @Test
    public void testInspect_ZipUnreadableFallsBackToUnzippedDir() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("grade.properties", "time=1\nmemory=256\n");
        entries.put("statement/task.pdf", "%PDF");
        entries.put("author/author.cpp", "int main(){}");
        entries.put("tests/p.01.in", "1\n");
        entries.put("tests/p.01.sol", "1\n");
        File goodZip = createZip("good.zip", entries);
        File unzipped = folder.newFolder("fallback-unzipped");
        ProblemZipUtils.extractSafely(goodZip, unzipped);
        File corruptZip = folder.newFile("corrupt2.zip");
        FileUtils.writeStringToFile(corruptZip, "not-a-valid-zip", StandardCharsets.UTF_8);

        ProblemPackageInfoDTO info = ProblemPackageInspector.inspect(corruptZip, unzipped, "task");

        assertThat(info.isHasPackage()).isTrue();
        assertThat(info.getTestCount()).isEqualTo(1);
        assertThat(info.isTestsCleanedUp()).isFalse();
        assertThat(info.getWarnings()).anyMatch(w -> w.contains("problem.zip could not be read"));
    }

    @Test
    public void testInspect_GraderPathKeepsFileNameCaseAndShowsDirForMultiLanguage() throws IOException {
        Map<String, String> single = new LinkedHashMap<>();
        single.put("grade.properties", "time=1\nmemory=256\n");
        single.put("system/Grader.CPP", "int main(){}");
        single.put("tests/p.01.in", "1\n");
        single.put("tests/p.01.sol", "1\n");
        ProblemPackageInfoDTO singleInfo = ProblemPackageInspector.inspect(createZip("single.zip", single), null, "p");
        assertThat(singleInfo.getGraderPath()).isEqualTo("system/Grader.CPP");

        Map<String, String> multi = new LinkedHashMap<>();
        multi.put("grade.properties", "time=1\nmemory=256\nextensions=cpp,java\n");
        multi.put("graders/cpp/grader.cpp", "int main(){}");
        multi.put("graders/java/Grader.java", "class Grader {}");
        multi.put("tests/p.01.in", "1\n");
        multi.put("tests/p.01.sol", "1\n");
        ProblemPackageInfoDTO multiInfo = ProblemPackageInspector.inspect(createZip("multi.zip", multi), null, "p");
        assertThat(multiInfo.getGraderPath()).isEqualTo("graders");
    }

    private void writeZipEntry(ZipOutputStream zos, String name, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private File createZip(String name, Map<String, String> entries) throws IOException {
        File zipFile = folder.newFile(name);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zipFile;
    }
}
