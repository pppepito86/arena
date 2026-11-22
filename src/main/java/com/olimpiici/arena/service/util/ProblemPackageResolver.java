package com.olimpiici.arena.service.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves standard problem package files (properties, statement PDF, author solution)
 * from both legacy Arena layouts and BOS/INFOS package layouts.
 */
public final class ProblemPackageResolver {

    private static final List<String> PROPERTIES_PRECEDENCE = Arrays.asList(
        "grade.properties",
        "task.properties",
        "grade.txt",
        "task.txt"
    );

    private static final Set<String> STANDARD_STATEMENT_FILENAMES = setOf(
        "task.pdf",
        "description.pdf",
        "statement.pdf"
    );

    private static final Pattern FIGURE_PDF = Pattern.compile("(^|/)(svg-inkscape|images?|imgs?|figs|figures|pictures|diagrams)/|_svg-raw\\.pdf$");
    private static final Pattern NON_STATEMENT_NAME = Pattern.compile("analysis|analisys|analiz|editorial|solution|reshenie|^author$");
    private static final Pattern NON_STATEMENT_DIR = Pattern.compile("^(analysis|analisys|analiz|editorial|reshenie|contestant|author|authors|solutions?|sol)$");
    private static final Pattern BULGARIAN_PDF = Pattern.compile("(^|/)bg/(?![^/]*[_.-]en\\.pdf$)|(^|[_./-])bg\\.pdf$");
    private static final Pattern CAMEL_CASE_BG_PDF = Pattern.compile("[a-z]BG\\.pdf$");

    private ProblemPackageResolver() {
    }

    /**
     * Walks {@code rootDir} safely (with try-with-resources) and returns forward-slash
     * relative paths of all regular files.
     */
    public static List<String> listRelativeFiles(File rootDir) throws IOException {
        if (rootDir == null || !rootDir.isDirectory()) {
            return Collections.emptyList();
        }
        Path root = rootDir.toPath();
        List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream
                .filter(Files::isRegularFile)
                .map(p -> toForwardSlash(root.relativize(p).toString()))
                .forEach(result::add);
        }
        return result;
    }

    /**
     * Finds the authoritative properties file path from a collection of relative paths
     * (either from disk or ZIP entries).
     * <p>
     * Precedence matches {@code grader}'s PropertiesFinder candidate set:
     * {@code grade.properties > task.properties > grade.txt > task.txt}, then shallowest,
     * shortest, and lexicographical path. Ignores {@code __MACOSX} and dotfiles.
     */
    public static Optional<String> findPropertiesEntry(Collection<String> relativePaths) {
        return findAllPropertiesEntries(relativePaths).stream().findFirst();
    }

    /**
     * Returns all candidate properties files ordered by precedence:
     * {@code grade.properties > task.properties > grade.txt > task.txt}, then shallowest,
     * shortest, and lexicographical path.
     */
    public static List<String> findAllPropertiesEntries(Collection<String> relativePaths) {
        if (relativePaths == null || relativePaths.isEmpty()) {
            return Collections.emptyList();
        }
        return relativePaths.stream()
            .filter(p -> !isIgnoredPath(p))
            .filter(p -> propertiesRank(fileName(p)) >= 0)
            .sorted(Comparator
                .comparingInt((String p) -> propertiesRank(fileName(p)))
                .thenComparingInt(ProblemPackageResolver::pathDepth)
                .thenComparingInt(String::length)
                .thenComparing(Comparator.naturalOrder()))
            .collect(Collectors.toList());
    }

    /**
     * Picks the statement PDF to serve:
     * <ol>
     * <li>drop {@code __MACOSX}/dotfiles, figure PDFs and editorials/solutions/contestant material
     *     (PDFs inside a {@code statement*} directory are always kept, except figures);</li>
     * <li>prefer Bulgarian ({@code *_bg.pdf}, {@code *-bg.pdf}, {@code *.bg.pdf}, {@code *BG.pdf},
     *     {@code bg.pdf}, or a {@code bg/} directory);</li>
     * <li>then the shallowest, then shortest path (close to the rule Arena always used, so
     *     root-level override PDFs keep winning), with standard names ({@code task.pdf}, ...)
     *     breaking ties.</li>
     * </ol>
     * If nothing is left, legacy packages that keep their only statement under {@code author/}
     * are still served.
     */
    public static Optional<String> findStatementPdf(Collection<String> relativePaths) {
        if (relativePaths == null) {
            return Optional.empty();
        }
        List<String> pdfs = relativePaths.stream()
            .filter(p -> !isIgnoredPath(p) && p.toLowerCase(Locale.ROOT).endsWith(".pdf"))
            .filter(p -> !FIGURE_PDF.matcher(normalizeRelativePath(p).toLowerCase(Locale.ROOT)).find())
            .collect(Collectors.toList());
        List<String> candidates = pdfs.stream().filter(p -> !isNonStatementPdf(p, false)).collect(Collectors.toList());
        if (candidates.isEmpty()) {
            candidates = pdfs.stream().filter(p -> !isNonStatementPdf(p, true)).collect(Collectors.toList());
        }
        List<String> bulgarian = candidates.stream().filter(ProblemPackageResolver::isBulgarianPdf).collect(Collectors.toList());
        return (bulgarian.isEmpty() ? candidates : bulgarian).stream()
            .min(Comparator.comparingInt(ProblemPackageResolver::pathDepth)
                .thenComparingInt(String::length)
                .thenComparingInt(p -> STANDARD_STATEMENT_FILENAMES.contains(fileName(p).toLowerCase(Locale.ROOT)) ? 0 : 1)
                .thenComparing(Comparator.naturalOrder()));
    }

    private static boolean isNonStatementPdf(String rawPath, boolean allowAuthorDir) {
        String[] segments = normalizeRelativePath(rawPath).toLowerCase(Locale.ROOT).split("/");
        String stem = stripExtension(segments[segments.length - 1]);
        // A problem literally named e.g. "analysis" is wrapped in analysis/ and has analysis.pdf.
        boolean namedAfterWrapper = segments.length > 1 && stem.equals(segments[0]);
        if (!namedAfterWrapper && NON_STATEMENT_NAME.matcher(stem).find()) {
            return true;
        }
        boolean inStatementDir = Arrays.stream(segments, 0, segments.length - 1).anyMatch(d -> d.startsWith("statement"));
        return !inStatementDir && Arrays.stream(segments, 0, segments.length - 1)
            .anyMatch(d -> NON_STATEMENT_DIR.matcher(d).find() && !(allowAuthorDir && d.equals("author")));
    }

    private static boolean isBulgarianPdf(String rawPath) {
        String p = normalizeRelativePath(rawPath);
        return BULGARIAN_PDF.matcher(p.toLowerCase(Locale.ROOT)).find() || CAMEL_CASE_BG_PDF.matcher(p).find();
    }

    static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /**
     * Splits a filename stem into lowercase tokens on non-alphanumeric delimiters and
     * camelCase boundaries (e.g., {@code authorTest} -> {@code [author, test]}).
     */
    static boolean isIgnoredPath(String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return true;
        }
        String normalized = normalizeRelativePath(rawPath);
        if (normalized.isEmpty()) {
            return true;
        }
        String[] segments = normalized.split("/");
        for (String segment : segments) {
            if ("__MACOSX".equalsIgnoreCase(segment) || (segment.startsWith(".") && !".".equals(segment))) {
                return true;
            }
        }
        return segments[segments.length - 1].isEmpty();
    }

    public static String normalizeRelativePath(String path) {
        String p = toForwardSlash(path);
        while (p.startsWith("./") || p.startsWith("/")) {
            p = p.startsWith("./") ? p.substring(2) : p.substring(1);
        }
        return p;
    }

    static String toForwardSlash(String path) {
        return path.replace('\\', '/');
    }

    static String fileName(String rawPath) {
        String p = normalizeRelativePath(rawPath);
        int idx = p.lastIndexOf('/');
        return idx >= 0 ? p.substring(idx + 1) : p;
    }

    static int pathDepth(String rawPath) {
        String p = normalizeRelativePath(rawPath);
        int count = 0;
        for (int i = 0; i < p.length(); i++) {
            if (p.charAt(i) == '/') {
                count++;
            }
        }
        return count;
    }

    private static int propertiesRank(String name) {
        return PROPERTIES_PRECEDENCE.indexOf(name.toLowerCase(Locale.ROOT));
    }

    private static Set<String> setOf(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    }
