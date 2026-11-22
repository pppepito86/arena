package com.olimpiici.arena.service.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Safe ZIP helpers for problem packages:
 * - Charset fallback (UTF-8, then CP866 or windows-1251 chosen heuristically, then ISO-8859-1)
 *   for Bulgarian Windows archives
 * - Normalized-path Zip-Slip validation during extraction
 * - Atomic stream-based single-entry updates without extracting or shelling out to `zip`
 */
public final class ProblemZipUtils {

    private static final List<Charset> LEGACY_CYRILLIC_CHARSETS = Arrays.asList(
        Charset.forName("IBM866"),
        Charset.forName("windows-1251")
    );

    private ProblemZipUtils() {
    }

    public static final class OpenedZip implements AutoCloseable {
        private final ZipFile zipFile;
        private final Charset charset;

        OpenedZip(ZipFile zipFile, Charset charset) {
            this.zipFile = zipFile;
            this.charset = charset;
        }

        public ZipFile getZipFile() {
            return zipFile;
        }

        public Charset getCharset() {
            return charset;
        }

        @Override
        public void close() throws IOException {
            zipFile.close();
        }
    }

    /**
     * Opens a ZIP archive, trying UTF-8 first. If the entry names are not valid UTF-8, the
     * archive is reopened as ISO-8859-1 (which maps every byte 1:1, so it never fails on names)
     * to recover the raw name bytes, and the best legacy Cyrillic charset (CP866 or
     * windows-1251) is picked heuristically. If neither decodes the names plausibly, the
     * ISO-8859-1 view is returned so that names still round-trip byte-for-byte on rewrite.
     *
     * <p>Note: Java 8 reports malformed names lazily as {@link IllegalArgumentException} while
     * iterating entries; Java 9+ rejects them eagerly in the {@link ZipFile} constructor with a
     * {@link java.util.zip.ZipException}. Both are treated as a charset mismatch here; a genuinely
     * corrupt archive is detected by the ISO-8859-1 attempt failing as well.
     */
    public static OpenedZip openZipFile(File file) throws IOException {
        Exception utf8Error;
        try {
            return openFully(file, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | IOException e) {
            utf8Error = e;
        }

        OpenedZip latin1;
        try {
            latin1 = openFully(file, StandardCharsets.ISO_8859_1);
        } catch (IllegalArgumentException | IOException e) {
            if (utf8Error instanceof IOException) {
                utf8Error.addSuppressed(e);
                throw (IOException) utf8Error;
            }
            throw new IOException("Cannot read ZIP archive " + file.getName(), e);
        }

        Charset best;
        try {
            best = detectLegacyCharset(rawEntryNames(latin1));
        } catch (RuntimeException e) {
            latin1.close();
            throw e;
        }
        if (best.equals(StandardCharsets.ISO_8859_1)) {
            return latin1;
        }
        latin1.close();
        try {
            return openFully(file, best);
        } catch (IllegalArgumentException | IOException e) {
            // Should not happen (the names were already validated against this charset),
            // but never fail an otherwise readable archive because of the heuristic.
            return openFully(file, StandardCharsets.ISO_8859_1);
        }
    }

    private static OpenedZip openFully(File file, Charset charset) throws IOException {
        ZipFile zf = new ZipFile(file, charset);
        try {
            // Force full central-directory decoding so malformed names fail here, not mid-iteration.
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                entries.nextElement();
            }
            return new OpenedZip(zf, charset);
        } catch (RuntimeException e) {
            closeQuietly(zf);
            throw e;
        }
    }

    /**
     * Recovers the raw on-disk name bytes from an archive opened as ISO-8859-1. Entries that
     * carry the UTF-8 (EFS) flag are decoded as UTF-8 by the JDK regardless of the requested
     * charset; they are already correct, so they are skipped.
     */
    private static List<byte[]> rawEntryNames(OpenedZip latin1) {
        List<byte[]> result = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = latin1.getZipFile().entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (name.chars().allMatch(c -> c <= 0xFF)) {
                result.add(name.getBytes(StandardCharsets.ISO_8859_1));
            }
        }
        return result;
    }

    /**
     * Picks the legacy charset that decodes the given raw names most plausibly as Bulgarian.
     *
     * <p>Windows' built-in "Compressed folder" and 7-Zip (by default) store names in the OEM
     * code page, which is CP866 for Cyrillic locales; some other tools use the ANSI code page
     * windows-1251. Both decode almost any byte sequence without error, so the choice is made by
     * scoring: decoded Cyrillic letters (U+0410..U+044F) count for; other non-ASCII characters
     * (box drawing, Ukrainian/Serbian letters, punctuation) and Cyrillic letters glued to Latin
     * letters (typical mojibake of Western European names) count against. Text in the wrong one
     * of the two code pages always produces some of the latter. Ties go to CP866.
     */
    static Charset detectLegacyCharset(List<byte[]> rawNames) {
        Charset bestCharset = StandardCharsets.ISO_8859_1;
        int bestScore = 0;
        for (Charset candidate : LEGACY_CYRILLIC_CHARSETS) {
            Integer score = scoreCyrillic(rawNames, candidate);
            if (score != null && score > bestScore) {
                bestScore = score;
                bestCharset = candidate;
            }
        }
        return bestCharset;
    }

    /** @return the plausibility score, or {@code null} if some name is not decodable. */
    private static Integer scoreCyrillic(List<byte[]> rawNames, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        int score = 0;
        for (byte[] raw : rawNames) {
            CharBuffer decoded;
            try {
                decoded = decoder.reset().decode(ByteBuffer.wrap(raw));
            } catch (CharacterCodingException e) {
                return null;
            }
            for (int i = 0; i < decoded.length(); i++) {
                char c = decoded.charAt(i);
                if (c < 0x80) {
                    continue;
                }
                if (c >= '\u0410' && c <= '\u044F' && !isAsciiLetterAt(decoded, i - 1) && !isAsciiLetterAt(decoded, i + 1)) {
                    score += 1;
                } else {
                    score -= 3;
                }
            }
        }
        return score;
    }

    private static boolean isAsciiLetterAt(CharSequence s, int i) {
        if (i < 0 || i >= s.length()) {
            return false;
        }
        char c = s.charAt(i);
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    /**
     * Lists all non-directory entry names in the ZIP archive.
     */
    public static List<String> listFileEntries(File zipFile) throws IOException {
        try (OpenedZip opened = openZipFile(zipFile)) {
            return listFileEntries(opened);
        }
    }

    public static List<String> listFileEntries(OpenedZip opened) {
        List<String> result = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = opened.getZipFile().entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                result.add(entry.getName());
            }
        }
        return result;
    }

    /**
     * Reads a single entry's raw bytes from a ZIP archive.
     */
    public static Optional<byte[]> readEntry(File zipFile, String entryName) throws IOException {
        try (OpenedZip opened = openZipFile(zipFile)) {
            return readEntry(opened, entryName);
        }
    }

    public static Optional<byte[]> readEntry(OpenedZip opened, String entryName) throws IOException {
        ZipEntry entry = opened.getZipFile().getEntry(entryName);
        if (entry == null || entry.isDirectory()) {
            return Optional.empty();
        }
        try (InputStream is = opened.getZipFile().getInputStream(entry)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(is, baos);
            return Optional.of(baos.toByteArray());
        }
    }

    /**
     * Extracts all entries of {@code zipFile} into {@code targetDir} with Zip-Slip validation.
     * Rejects empty archives (0 file entries) so an empty ZIP cannot overwrite a working problem.
     */
    public static void extractSafely(File zipFile, File targetDir) throws IOException {
        Files.createDirectories(targetDir.toPath());
        Path canonicalRoot = targetDir.toPath().toAbsolutePath().normalize();
        int extractedFiles = 0;

        try (OpenedZip opened = openZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = opened.getZipFile().entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String normalizedName = entry.getName().replace('\\', '/');
                if (normalizedName.isEmpty() || "/".equals(normalizedName)) {
                    continue;
                }
                Path resolved = canonicalRoot.resolve(normalizedName).normalize();
                if (!resolved.startsWith(canonicalRoot)) {
                    throw new IOException("Blocked Zip-Slip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Path parent = resolved.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    try (InputStream is = opened.getZipFile().getInputStream(entry);
                         OutputStream os = Files.newOutputStream(resolved)) {
                        IOUtils.copy(is, os);
                    }
                    extractedFiles++;
                }
            }
        }

        if (extractedFiles == 0) {
            throw new IOException("ZIP archive contains no files: " + zipFile.getName());
        }
    }

    /**
     * Stream-copies {@code zipFile} into a sibling temporary file, replacing {@code entryName}
     * (or adding it if absent), and atomically swaps it into place. Deduplicates repeated
     * central-directory entries so repackaged ZIPs do not fail with duplicate entry errors.
     */
    public static void updateOrAddEntry(File zipFile, String entryName, byte[] content) throws IOException {
        String normalizedTarget = entryName.replace('\\', '/');
        File parentDir = zipFile.getParentFile();
        if (parentDir != null) {
            Files.createDirectories(parentDir.toPath());
        }
        File tempZip = new File(parentDir, zipFile.getName() + ".tmp." + UUID.randomUUID());
        boolean replaced = false;
        Set<String> writtenNames = new HashSet<>();

        try {
            try (OpenedZip opened = openZipFile(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip), opened.getCharset())) {
                Enumeration<? extends ZipEntry> entries = opened.getZipFile().entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String normalizedCurrent = entry.getName().replace('\\', '/');
                    if (!entry.isDirectory() && normalizedCurrent.equals(normalizedTarget)) {
                        if (!replaced && writtenNames.add(entry.getName())) {
                            ZipEntry updated = new ZipEntry(entry.getName());
                            updated.setTime(System.currentTimeMillis());
                            zos.putNextEntry(updated);
                            zos.write(content);
                            zos.closeEntry();
                            replaced = true;
                        }
                        continue;
                    }
                    if (!writtenNames.add(entry.getName())) {
                        continue;
                    }
                    ZipEntry copy = new ZipEntry(entry.getName());
                    if (entry.getTime() != -1L) {
                        copy.setTime(entry.getTime());
                    }
                    zos.putNextEntry(copy);
                    if (!entry.isDirectory()) {
                        try (InputStream is = opened.getZipFile().getInputStream(entry)) {
                            IOUtils.copy(is, zos);
                        }
                    }
                    zos.closeEntry();
                }
                if (!replaced) {
                    ZipEntry added = new ZipEntry(normalizedTarget);
                    added.setTime(System.currentTimeMillis());
                    zos.putNextEntry(added);
                    zos.write(content);
                    zos.closeEntry();
                }
            }
            moveAtomically(tempZip.toPath(), zipFile.toPath());
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tempZip.toPath());
            throw e;
        }
    }

    public static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void closeQuietly(ZipFile zf) {
        if (zf != null) {
            try {
                zf.close();
            } catch (IOException ignored) {
                // ignore
            }
        }
    }
}
