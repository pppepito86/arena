package com.olimpiici.arena.service.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProblemZipUtilsTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testExtractSafely_Windows1251BulgarianFilenames() throws IOException {
        File zipFile = folder.newFile("bg.zip");
        Charset cp1251 = Charset.forName("windows-1251");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile), cp1251)) {
            ZipEntry entry = new ZipEntry("условие/задача.txt");
            zos.putNextEntry(entry);
            zos.write("hello".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        File destDir = folder.newFolder("out");
        ProblemZipUtils.extractSafely(zipFile, destDir);

        File extracted = new File(destDir, "условие/задача.txt");
        assertThat(extracted).isFile();
        assertThat(FileUtils.readFileToString(extracted, StandardCharsets.UTF_8)).isEqualTo("hello");
    }

    @Test
    public void testExtractSafely_Cp866BulgarianFilenames() throws IOException {
        // Windows "Send to -> Compressed folder" and 7-Zip store names in the OEM code page.
        File zipFile = createZip("bg866.zip", Charset.forName("IBM866"),
            "Условие/задача.pdf", "tests/вход.01.in");

        File destDir = folder.newFolder("out866");
        ProblemZipUtils.extractSafely(zipFile, destDir);

        assertThat(new File(destDir, "Условие/задача.pdf")).isFile();
        assertThat(new File(destDir, "tests/вход.01.in")).isFile();
    }

    @Test
    public void testOpenZipFile_DetectsCharset() throws IOException {
        File utf8 = createZip("utf8.zip", StandardCharsets.UTF_8, "условие.pdf");
        File cp866 = createZip("cp866.zip", Charset.forName("IBM866"), "условие.pdf", "решение.cpp");
        File cp1251 = createZip("cp1251.zip", Charset.forName("windows-1251"), "условие.pdf", "решение.cpp");
        File ascii = createZip("ascii.zip", StandardCharsets.US_ASCII, "tests/test.01.in");

        assertCharset(utf8, StandardCharsets.UTF_8);
        assertCharset(cp866, Charset.forName("IBM866"));
        assertCharset(cp1251, Charset.forName("windows-1251"));
        assertCharset(ascii, StandardCharsets.UTF_8);
        assertThat(ProblemZipUtils.listFileEntries(cp866)).containsExactlyInAnyOrder("условие.pdf", "решение.cpp");
        assertThat(ProblemZipUtils.listFileEntries(cp1251)).containsExactlyInAnyOrder("условие.pdf", "решение.cpp");
    }

    @Test
    public void testDetectLegacyCharset_FallsBackToLatin1ForNonCyrillic() {
        // German umlauts in windows-1252: neither CP866 nor windows-1251 yields Cyrillic text.
        byte[] raw = "Lösung_für_Prüfung.txt".getBytes(Charset.forName("windows-1252"));
        assertThat(ProblemZipUtils.detectLegacyCharset(Collections.singletonList(raw)))
            .isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    public void testUpdateOrAddEntry_PreservesLegacyEntryNameBytes() throws IOException {
        Charset cp866 = Charset.forName("IBM866");
        File zipFile = createZip("rt866.zip", cp866, "условие.pdf", "grade.properties");

        ProblemZipUtils.updateOrAddEntry(zipFile, "grade.properties", "time=2\n".getBytes(StandardCharsets.UTF_8));

        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zipFile, cp866)) {
            assertThat(zf.getEntry("условие.pdf")).isNotNull();
        }
        assertThat(new String(ProblemZipUtils.readEntry(zipFile, "grade.properties").get(), StandardCharsets.UTF_8))
            .isEqualTo("time=2\n");
    }

    @Test
    public void testOpenZipFile_CorruptArchiveThrowsIOException() throws IOException {
        File notAZip = folder.newFile("garbage.zip");
        FileUtils.writeByteArrayToFile(notAZip, new byte[] { 1, 2, 3, 4, 5 });

        assertThatThrownBy(() -> ProblemZipUtils.openZipFile(notAZip)).isInstanceOf(IOException.class);
    }

    private void assertCharset(File zipFile, Charset expected) throws IOException {
        try (ProblemZipUtils.OpenedZip opened = ProblemZipUtils.openZipFile(zipFile)) {
            assertThat(opened.getCharset()).isEqualTo(expected);
        }
    }

    private File createZip(String name, Charset charset, String... entryNames) throws IOException {
        File zipFile = folder.newFile(name);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile), charset)) {
            for (String entryName : entryNames) {
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write("x".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zipFile;
    }

    @Test
    public void testExtractSafely_RejectsZipSlip() throws IOException {
        File zipFile = folder.newFile("evil.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("../escaped.txt"));
            zos.write("bad".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        File destDir = folder.newFolder("target");
        assertThatThrownBy(() -> ProblemZipUtils.extractSafely(zipFile, destDir))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Zip-Slip");
        assertThat(new File(destDir.getParentFile(), "escaped.txt")).doesNotExist();
    }

    @Test
    public void testUpdateOrAddEntry_ReplacesExistingAndPreservesOtherEntries() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("grade.properties", "time=1\nmemory=256\ngroups=1-5\n");
        entries.put("tests/test.01.in", "1 2\n");
        entries.put("tests/test.01.sol", "3\n");
        File zipFile = createZip("problem.zip", entries);

        byte[] newProps = "time=2.5\nmemory=512\ngroups=1-5\n".getBytes(StandardCharsets.UTF_8);
        ProblemZipUtils.updateOrAddEntry(zipFile, "grade.properties", newProps);

        assertThat(ProblemZipUtils.listFileEntries(zipFile))
            .containsExactlyInAnyOrder("grade.properties", "tests/test.01.in", "tests/test.01.sol");
        assertThat(new String(ProblemZipUtils.readEntry(zipFile, "grade.properties").get(), StandardCharsets.UTF_8))
            .isEqualTo("time=2.5\nmemory=512\ngroups=1-5\n");
        assertThat(new String(ProblemZipUtils.readEntry(zipFile, "tests/test.01.in").get(), StandardCharsets.UTF_8))
            .isEqualTo("1 2\n");
    }

    @Test
    public void testUpdateOrAddEntry_AddsNewEntryWhenMissing() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("tests/test.01.in", "1 2\n");
        File zipFile = createZip("no-props.zip", entries);

        ProblemZipUtils.updateOrAddEntry(zipFile, "grade.properties", "time=1.5\n".getBytes(StandardCharsets.UTF_8));

        assertThat(ProblemZipUtils.listFileEntries(zipFile))
            .containsExactlyInAnyOrder("tests/test.01.in", "grade.properties");
        assertThat(new String(ProblemZipUtils.readEntry(zipFile, "grade.properties").get(), StandardCharsets.UTF_8))
            .isEqualTo("time=1.5\n");
    }

    @Test
    public void testExtractSafely_RejectsEmptyZip() throws IOException {
        File zipFile = createZip("empty.zip", new LinkedHashMap<>());
        File destDir = folder.newFolder("empty-out");

        assertThatThrownBy(() -> ProblemZipUtils.extractSafely(zipFile, destDir))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("no files");
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
