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

    private static final Set<String> SOLUTION_DIRS = setOf(
        "author",
        "authors",
        "solution",
        "solutions",
        "sol"
    );

    private static final Set<String> SEMANTIC_PACKAGE_DIRS = setOf(
        "contestant", "analysis", "analiz", "analisys", "editorial",
        "author", "authors", "solution", "solutions", "sol", "reshenie",
        "checker", "system", "manager", "tests", "test",
        "statement", "statements", "translations", "grader", "graders",
        "images", "img", "imgs", "figs", "figures", "pictures", "diagrams", "svg-inkscape",
        "testgen", "gen", "gens", "generator", "generators",
        "validator", "validators", "cheats", "cheat", "wrong", "partial", "subtasks"
    );

    private static final Set<String> PROBLEM_ROOT_MARKER_DIRS = setOf(
        "statement", "statements", "tests", "test"
    );

    private static final Set<String> EXCLUDED_AUTHOR_DIRS = setOf(
        "contestant",
        "checker",
        "system",
        "manager",
        "testgen",
        "gen",
        "gens",
        "generator",
        "generators",
        "validator",
        "validators",
        "tests",
        "test",
        "statement",
        "statements",
        "analysis",
        "editorial",
        "cheats",
        "cheat",
        "wrong",
        "partial",
        "subtasks"
    );

    private static final Set<String> EXCLUDED_AUTHOR_TOKENS = setOf(
        "grader",
        "lgrader",
        "checker",
        "manager",
        "validator",
        "validation",
        "generator",
        "testgen",
        "helper",
        "stresstest",
        "stress",
        "cheat",
        "wrong",
        "wa",
        "tl",
        "tle",
        "ml",
        "mle",
        "slow",
        "naive",
        "brute",
        "bruteforce",
        "subtask",
        "bad",
        "dummy",
        "fake",
        "bf",
        "exp",
        "nq",
        "n2",
        "n3",
        "n4",
        "quadratic",
        "cubic"
    );

    private static final Set<String> FULL_SOLUTION_TOKENS = setOf(
        "full",
        "opt",
        "model",
        "official",
        "ac"
    );

    private static final Pattern SCORE_100_PATTERN = Pattern.compile("(^|[^0-9])100(p|pts)?([^0-9]|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARTIAL_POINTS_TOKEN = Pattern.compile("^(\\d{1,2}p|[1-9]\\d|sub\\d+|st\\d+|subtask\\d+|val\\d+|validation\\d+)$");
    private static final Pattern AUTHOR_INDEXED_TOKEN = Pattern.compile("^author\\d+$");

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

    /**
     * Finds the author's full solution file across {@code allowedExtensions}, excluding
     * contestant stubs, checkers, graders, generators/validators, partial/cheat/wrong
     * solutions, {@code __MACOSX}, and dotfiles.
     */
    public static Optional<String> findAuthorSolution(
            Collection<String> relativePaths,
            List<String> allowedExtensions,
            String problemTitle) {
        if (relativePaths == null || relativePaths.isEmpty() || allowedExtensions == null || allowedExtensions.isEmpty()) {
            return Optional.empty();
        }
        List<String> validPaths = relativePaths.stream()
            .filter(p -> !isIgnoredPath(p))
            .collect(Collectors.toList());
        if (validPaths.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> wrapperDir = detectSingleWrapperDir(validPaths);
        String wrapperHint = wrapperDir.map(ProblemPackageResolver::stripLeadingProblemCode).orElse(null);
        String titleHint = normalizeTitleHint(problemTitle);

        List<String> normalizedExts = allowedExtensions.stream()
            .filter(e -> e != null && !e.trim().isEmpty())
            .map(e -> e.trim().toLowerCase(Locale.ROOT))
            .distinct()
            .collect(Collectors.toList());
        if (normalizedExts.isEmpty()) {
            normalizedExts = Collections.singletonList("cpp");
        }
        final List<String> exts = normalizedExts;

        List<AuthorCandidate> candidates = new ArrayList<>();
        for (String p : validPaths) {
            int extRank = extensionRank(fileName(p), exts);
            if (extRank < 0) {
                continue;
            }
            String stripped = stripWrapper(p, wrapperDir);
            if (!isExcludedAuthorCandidate(stripped, titleHint, wrapperHint)) {
                candidates.add(new AuthorCandidate(p, stripped, extRank, titleHint, wrapperHint));
            }
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        candidates.sort(Comparator
            .comparingInt((AuthorCandidate c) -> c.inSolutionDir ? 0 : 1)
            .thenComparingInt(c -> c.nameRank)
            .thenComparingInt(c -> c.extRank)
            .thenComparingInt(c -> c.depth)
            .thenComparingInt(c -> c.raw.length())
            .thenComparing(c -> c.raw));

        AuthorCandidate best = candidates.get(0);
        if (best.nameRank <= 5) {
            return Optional.of(best.raw);
        }

        // No strong naming signal on the top candidate: prefer an unambiguous sole solution-dir candidate
        List<AuthorCandidate> inSolutionDir = candidates.stream()
            .filter(c -> c.inSolutionDir)
            .collect(Collectors.toList());
        if (inSolutionDir.size() == 1) {
            return Optional.of(inSolutionDir.get(0).raw);
        }

        // Otherwise check if a candidate outside solution dirs carries a strong naming signal (e.g. root author.cpp)
        Optional<AuthorCandidate> strongOutside = candidates.stream()
            .filter(c -> c.nameRank <= 5)
            .findFirst();
        if (strongOutside.isPresent()) {
            return Optional.of(strongOutside.get().raw);
        }

        return candidates.size() == 1 ? Optional.of(best.raw) : Optional.empty();
    }

    private static boolean isExcludedAuthorCandidate(String strippedPath, String titleHint, String wrapperHint) {
        String[] segments = normalizeRelativePath(strippedPath).split("/");
        String name = segments[segments.length - 1];
        String ext = fileExtension(name);
        boolean inSolutionDir = false;

        for (int i = 0; i < segments.length - 1; i++) {
            String dir = segments[i].toLowerCase(Locale.ROOT);
            if (EXCLUDED_AUTHOR_DIRS.contains(dir)) {
                return true;
            }
            if (SOLUTION_DIRS.contains(dir)) {
                inSolutionDir = true;
            }
        }

        String stem = stripExtension(name);
        String lowerStem = stem.toLowerCase(Locale.ROOT);

        // For output-only packages (zip/txt), never pick random non-solution archives (e.g. contestant/imgs.zip, readme.txt),
        // but still allow explicit root-level author.<ext> or solution.<ext> for legacy packages.
        if (("zip".equals(ext) || "txt".equals(ext)) && !inSolutionDir
                && !"author".equals(lowerStem) && !"solution".equals(lowerStem)) {
            return true;
        }

        if ((titleHint != null && lowerStem.equals(titleHint))
                || (wrapperHint != null && lowerStem.equals(wrapperHint))) {
            return false;
        }

        return splitIdentifierTokens(stem).stream()
            .anyMatch(token -> EXCLUDED_AUTHOR_TOKENS.contains(token) || PARTIAL_POINTS_TOKEN.matcher(token).matches());
    }

    private static boolean isInSolutionDir(String strippedPath) {
        String[] segments = normalizeRelativePath(strippedPath).split("/");
        for (int i = 0; i < segments.length - 1; i++) {
            if (SOLUTION_DIRS.contains(segments[i].toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Author candidate name priority:
     * 0 = exact author.<ext>
     * 1 = exact <title>.<ext>, <wrapperHint>.<ext>, or solution.<ext>
     * 2 = numeric-bounded 100 (e.g. belote_100p, alex_100)
     * 3 = explicit full-solution token (full, opt, model, official, ac)
     * 4 = generic author token (e.g. three-author, author1)
     * 5 = generic solution/sol token (e.g. encho_solution)
     * 6 = no specific naming signal
     */
    private static int authorNameRank(String fileName, String titleHint, String wrapperHint) {
        String stem = stripExtension(fileName);
        String lowerStem = stem.toLowerCase(Locale.ROOT);
        if ("author".equals(lowerStem)) {
            return 0;
        }
        if ("solution".equals(lowerStem)
                || (titleHint != null && lowerStem.equals(titleHint))
                || (wrapperHint != null && lowerStem.equals(wrapperHint))) {
            return 1;
        }
        if (SCORE_100_PATTERN.matcher(stem).find()) {
            return 2;
        }
        List<String> tokens = splitIdentifierTokens(stem);
        if (tokens.stream().anyMatch(FULL_SOLUTION_TOKENS::contains)) {
            return 3;
        }
        if (tokens.stream().anyMatch(t -> "author".equals(t) || AUTHOR_INDEXED_TOKEN.matcher(t).matches())) {
            return 4;
        }
        if (tokens.stream().anyMatch(t -> "solution".equals(t) || "sol".equals(t))) {
            return 5;
        }
        return 6;
    }

    private static int extensionRank(String fileName, List<String> allowedExts) {
        String ext = fileExtension(fileName);
        if (ext.isEmpty()) {
            return -1;
        }
        return allowedExts.indexOf(ext);
    }

    private static String fileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String normalizeTitleHint(String title) {
        if (title == null) {
            return null;
        }
        String trimmed = title.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

            /**
     * If every valid relative path starts with the same top-level directory segment
     * (e.g. {@code A1_belote/...}), returns that segment so resolvers can normalize layouts.
     * Does not strip known semantic package directories (e.g. {@code contestant/}, {@code analysis/})
     * unless they themselves wrap a standard problem root ({@code statement/} or {@code tests/}).
     */
    static Optional<String> detectSingleWrapperDir(Collection<String> validPaths) {
        String common = null;
        for (String raw : validPaths) {
            String p = normalizeRelativePath(raw);
            int slash = p.indexOf('/');
            if (slash <= 0) {
                return Optional.empty();
            }
            String top = p.substring(0, slash);
            if (common == null) {
                common = top;
            } else if (!common.equals(top)) {
                return Optional.empty();
            }
        }
        if (common == null) {
            return Optional.empty();
        }
        String commonLower = common.toLowerCase(Locale.ROOT);
        if (SEMANTIC_PACKAGE_DIRS.contains(commonLower)) {
            boolean wrapsProblemRoot = false;
            String prefix = common + "/";
            for (String raw : validPaths) {
                String p = normalizeRelativePath(raw);
                if (p.startsWith(prefix)) {
                    String rest = p.substring(prefix.length());
                    int nextSlash = rest.indexOf('/');
                    if (nextSlash > 0) {
                        String childDir = rest.substring(0, nextSlash).toLowerCase(Locale.ROOT);
                        if (PROBLEM_ROOT_MARKER_DIRS.contains(childDir) && !childDir.equals(commonLower)) {
                            wrapsProblemRoot = true;
                            break;
                        }
                    }
                }
            }
            if (!wrapsProblemRoot) {
                return Optional.empty();
            }
        }
        return Optional.of(common);
    }

    static String stripWrapper(String rawPath, Optional<String> wrapperDir) {
        String p = normalizeRelativePath(rawPath);
        if (wrapperDir.isPresent()) {
            String prefix = wrapperDir.get() + "/";
            if (p.startsWith(prefix)) {
                return p.substring(prefix.length());
            }
        }
        return p;
    }

    /**
     * Strips common contest prefixes like {@code A1_}, {@code a1_}, {@code B2_}, {@code AB21_}, {@code E12_}
     * from a top-level wrapper folder name to derive a clean problem name hint.
     */
    static String stripLeadingProblemCode(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        String stripped = trimmed.replaceFirst("^([A-Za-z]{1,2}\\d{1,2}|[A-Za-z])[_\\-]+", "");
        String result = stripped.isEmpty() ? trimmed : stripped;
        return result.toLowerCase(Locale.ROOT);
    }

    static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /**
     * Splits a filename stem into lowercase tokens on non-alphanumeric delimiters and
     * camelCase boundaries (e.g., {@code authorTest} -> {@code [author, test]}).
     */
    static List<String> splitIdentifierTokens(String stem) {
        if (stem == null || stem.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < stem.length(); i++) {
            char c = stem.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString().toLowerCase(Locale.ROOT));
                    current.setLength(0);
                }
                continue;
            }
            if (current.length() > 0
                    && Character.isUpperCase(c)
                    && Character.isLowerCase(stem.charAt(i - 1))) {
                tokens.add(current.toString().toLowerCase(Locale.ROOT));
                current.setLength(0);
            }
            current.append(c);
        }
        if (current.length() > 0) {
            tokens.add(current.toString().toLowerCase(Locale.ROOT));
        }
        return tokens;
    }

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

        private static final class AuthorCandidate {
        final String raw;
        final boolean inSolutionDir;
        final int nameRank;
        final int extRank;
        final int depth;

        AuthorCandidate(String raw, String stripped, int extRank, String titleHint, String wrapperHint) {
            this.raw = raw;
            this.inSolutionDir = isInSolutionDir(stripped);
            this.nameRank = authorNameRank(fileName(stripped), titleHint, wrapperHint);
            this.extRank = extRank;
            this.depth = pathDepth(stripped);
        }
    }
}
