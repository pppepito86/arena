package com.olimpiici.arena.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class ProblemPackageResolverTest {

    @Test
    public void testFindPropertiesEntry_WrappedTopLevelDirAndTestsSubdir() {
        assertThat(ProblemPackageResolver.findPropertiesEntry(Arrays.asList(
            "A1_belote/statement/belote.pdf",
            "A1_belote/grade.properties",
            "A1_belote/tests/belote.01.in"
        ))).contains("A1_belote/grade.properties");

        assertThat(ProblemPackageResolver.findPropertiesEntry(Arrays.asList(
            "author/author.cpp",
            "tests/grade.properties",
            "tests/cowboys.01.in"
        ))).contains("tests/grade.properties");
    }

    @Test
    public void testFindPropertiesEntry_IgnoresMacOsxAndDotfilesAndDotDirs() {
        assertThat(ProblemPackageResolver.findPropertiesEntry(Arrays.asList(
            "__MACOSX/grade.properties",
            "._grade.properties",
            ".git/grade.properties",
            ".hidden/task.properties",
            "eagers/grade.properties"
        ))).contains("eagers/grade.properties");
    }

    @Test
    public void testFindPropertiesEntry_PrefersGradePropertiesOverRootTaskTxt() {
        assertThat(ProblemPackageResolver.findPropertiesEntry(Arrays.asList(
            "task.txt",
            "A1_belote/grade.properties"
        ))).contains("A1_belote/grade.properties");

        assertThat(ProblemPackageResolver.findPropertiesEntry(Collections.singletonList("task.txt")))
            .contains("task.txt");
    }
}
