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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
}
