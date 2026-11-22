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

    @Test
    public void testFindStatementPdf_ExcludesAnalysisEditorialAuthorAndFigures() {
        // cowboys.zip: author/analysis.pdf (shorter) vs statement/cowboys.pdf
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "author/analysis.pdf",
            "statement/cowboys.pdf"
        ))).contains("statement/cowboys.pdf");

        // elections.zip: A2_elections/analysis/elections.pdf vs A2_elections/statement/elections.pdf
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "A2_elections/analysis/elections.pdf",
            "A2_elections/statement/elections.pdf"
        ))).contains("A2_elections/statement/elections.pdf");

        // primes.zip: editorial/primes.pdf vs statement/primes.pdf (equal length)
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "editorial/primes.pdf",
            "statement/primes.pdf"
        ))).contains("statement/primes.pdf");

        // paintrectangle.zip: typo analisys.pdf
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "E11_paintrectangle/author/analisys.pdf",
            "E11_paintrectangle/statement/paintrectangle.pdf"
        ))).contains("E11_paintrectangle/statement/paintrectangle.pdf");

        // monkey.zip: author/author.pdf
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "author/author.pdf",
            "statement/monkey.pdf"
        ))).contains("statement/monkey.pdf");

        // plans.zip: svg-inkscape figures + AppleDouble + __MACOSX
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "__MACOSX/plans/statement/._plans.pdf",
            "plans/statement/._plans.pdf",
            "plans/statement/svg-inkscape/fig1_svg-raw.pdf",
            "plans/statement/images/1.pdf",
            "plans/analysis/plans_analysis.pdf",
            "plans/statement/plans.pdf"
        ))).contains("plans/statement/plans.pdf");
    }

    @Test
    public void testFindStatementPdf_BulgarianFirstInMultiLanguageArchives() {
        // balance.zip: multiple _xx.pdf at same length + translations/am/balance_bg.pdf
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "analysis/analysis_bg.pdf",
            "statement/balance_he.pdf",
            "statement/balance_en.pdf",
            "statement/balance_bg.pdf",
            "statement/balance_sr.pdf",
            "statement/translations/am/balance_bg.pdf",
            "statement/translations/bg/balance_en.pdf"
        ))).contains("statement/balance_bg.pdf");

        // infection.zip: Bulgarian only under translations/bg/, foreign languages shorter in statement/
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "statement/infection_sr.pdf",
            "statement/infection_az.pdf",
            "statement/infection_ru.pdf",
            "statement/translations/bg/infection.pdf",
            "statement/translations/mk/infection.pdf"
        ))).contains("statement/translations/bg/infection.pdf");

        // eagers.zip: English eagers.pdf in statement/ vs eagers_bg.pdf in statement/ vs translations/bg/eagers_en.pdf
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "eagers/statement/eagers.pdf",
            "eagers/statement/eagers_bg.pdf",
            "eagers/statement/eagers_ru.pdf",
            "eagers/statement/translations/am/eagers_bg.pdf",
            "eagers/statement/translations/bg/eagers_en.pdf",
            "eagers/statement/translations/bg/eagers.pdf"
        ))).contains("eagers/statement/eagers_bg.pdf");

        // territory.zip: camelCase suffix territoryBG.pdf vs territoryEN.pdf
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "statement/territoryEN.pdf",
            "statement/territoryBG.pdf",
            "statement/territoryEN_ru.pdf",
            "statement/territoryEN_bg.pdf"
        ))).contains("statement/territoryBG.pdf");

        // Translations/bg beats un-suffixed English statement/
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "statement/problem.pdf",
            "statement/translations/bg/problem.pdf"
        ))).contains("statement/translations/bg/problem.pdf");
    }

    @Test
    public void testFindStatementPdf_EdgeCasesNoFalseExclusionsOrLangSuffixes() {
        // Problem literally named "analysis" wrapped in analysis/
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "analysis/statement/analysis.pdf",
            "analysis/analysis/analysis_editorial.pdf"
        ))).contains("analysis/statement/analysis.pdf");

        // Words ending in EN/BG/non-lang codes must not be misclassified as language suffixes
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "statement/DBG.pdf",
            "statement/task_bg.pdf"
        ))).contains("statement/task_bg.pdf");

        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "statement/GREEN.pdf",
            "statement/GREEN_ru.pdf"
        ))).contains("statement/GREEN.pdf");

        // Legacy Arena root layout: task.pdf beats sign.pdf
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "task.pdf",
            "sign.pdf"
        ))).contains("task.pdf");

        // Archive containing only analysis/ must NOT strip analysis/ as a wrapper dir
        assertThat(ProblemPackageResolver.findStatementPdf(Collections.singletonList(
            "analysis/notes.pdf"
        ))).isEmpty();

        // statement/bg/task.pdf and 2-letter statement/bg.pdf are recognized as Bulgarian
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "statement/en/task.pdf",
            "statement/bg/task.pdf"
        ))).contains("statement/bg/task.pdf");

        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "statement/en.pdf",
            "statement/bg.pdf",
            "statement/ru.pdf"
        ))).contains("statement/bg.pdf");

        // Lowercase wrapper prefix stripping
        assertThat(ProblemPackageResolver.stripLeadingProblemCode("a1_belote")).isEqualTo("belote");

        // Legacy Arena packages that store their only statement PDF under author/
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "author/shoes.pdf",
            "tests/shoes.01.in"
        ))).contains("author/shoes.pdf");

        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "AK3.compromise/author/statement/compromise_statement.pdf",
            "AK3.compromise/tests/01.in"
        ))).contains("AK3.compromise/author/statement/compromise_statement.pdf");

        // Normal statement/ PDF still beats any non-statement PDF in author/ (even with _bg suffix)
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "author/notes_bg.pdf",
            "statement/task.pdf"
        ))).contains("statement/task.pdf");

        // Root-level override PDF beats statement/ PDF of the same language tier (legacy Arena convention)
        assertThat(ProblemPackageResolver.findStatementPdf(Arrays.asList(
            "pdf.pdf",
            "statement/miners_statement.pdf"
        ))).contains("pdf.pdf");
    }

    @Test
    public void testFindAuthorSolution_ExcludesContestantStubsCheckersAndOutputOnlyJunk() {
        // Interactive problem (cleanup.zip): contestant/cleanup.cpp stub must NOT beat real 100p solution
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "cleanup/contestant/grader.cpp",
            "cleanup/contestant/cleanup.cpp",
            "cleanup/system/grader.cpp",
            "cleanup/checker/checker.cpp",
            "cleanup/author/cleanup_first_5p.cpp",
            "cleanup/author/cleanup_Iliyan_100p.cpp"
        ), Collections.singletonList("cpp"), "cleanup")).contains("cleanup/author/cleanup_Iliyan_100p.cpp");

        // special.zip: checker/checker.cpp is the ONLY .cpp -> must return empty
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "special/checker/checker.cpp",
            "special/grade.properties"
        ), Collections.singletonList("cpp"), "special")).isEmpty();

        // azul.zip: extensions=zip, contestant/imgs.zip must NOT be treated as author solution
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "AK1_azul/contestant/imgs.zip",
            "AK1_azul/checker/checker.cpp"
        ), Collections.singletonList("zip"), "azul")).isEmpty();
    }

    @Test
    public void testFindAuthorSolution_HandlesModernInfosNamingAndNullOrCyrillicTitles() {
        // infection.zip: author_opt.cpp beats author_n2.cpp and author_n3.cpp
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "contestant/infection.cpp",
            "author/validation_1.cpp",
            "author/author_n2.cpp",
            "author/author_n3.cpp",
            "author/author_opt.cpp"
        ), Collections.singletonList("cpp"), null)).contains("author/author_opt.cpp");

        // managers.zip: author/solution.cpp
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "author/NQ.cpp",
            "author/solution.cpp",
            "testgen/solution.cpp"
        ), Collections.singletonList("cpp"), "Мениджъри")).contains("author/solution.cpp");

        // walk.zip: solutions/sol_full.cpp (and "walk" must NOT be excluded by "wa"!)
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "solutions/cheat_init_zero.cpp",
            "solutions/sol_full.cpp"
        ), Collections.singletonList("cpp"), "walk")).contains("solutions/sol_full.cpp");

        // C1_three.zip: three-author.cpp with null title
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "C1_three/tests/testgen.cpp",
            "C1_three/author/three-bruteforce.cpp",
            "C1_three/author/three-dp-naive.cpp",
            "C1_three/author/three-author.cpp"
        ), Collections.singletonList("cpp"), null)).contains("C1_three/author/three-author.cpp");

        // abstract.zip: abstract100_MergeNow_0p.cpp must be excluded despite containing "100"
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "contestant/abstract.cpp",
            "author/abstract100_MergeNow_0p.cpp",
            "author/abstract_emil_nostop_65p.cpp"
        ), Collections.singletonList("cpp"), "abstract")).isEmpty();
    }

    @Test
    public void testFindAuthorSolution_NoSubstringFalseExclusionsAndMultiExtensions() {
        // Legitimate problem names containing "wa", "tl", "bad" substrings must NOT be excluded
        assertThat(ProblemPackageResolver.findAuthorSolution(
            Collections.singletonList("author/water.cpp"),
            Collections.singletonList("cpp"),
            "Вода"
        )).contains("author/water.cpp");

        assertThat(ProblemPackageResolver.findAuthorSolution(
            Collections.singletonList("author/battle.cpp"),
            Collections.singletonList("cpp"),
            "Битка"
        )).contains("author/battle.cpp");

        // Multi-extension: cpp,py finds author/author.py when no .cpp exists
        assertThat(ProblemPackageResolver.findAuthorSolution(
            Collections.singletonList("author/author.py"),
            Arrays.asList("cpp", "py"),
            "task"
        )).contains("author/author.py");

        // Legacy fixture: author/author.cpp beats author/sign.cpp, sign2.cpp
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "author/sign.cpp",
            "author/sign2.cpp",
            "author/author.cpp"
        ), Collections.singletonList("cpp"), "sign")).contains("author/author.cpp");

        // Archive containing only contestant/ must NOT strip contestant/ as a wrapper dir
        assertThat(ProblemPackageResolver.findAuthorSolution(
            Collections.singletonList("contestant/stub.cpp"),
            Collections.singletonList("cpp"),
            "task"
        )).isEmpty();

        // Output-only archive containing only author/ must keep author/ as a solution dir
        assertThat(ProblemPackageResolver.findAuthorSolution(
            Collections.singletonList("author/answers.zip"),
            Collections.singletonList("zip"),
            "task"
        )).contains("author/answers.zip");

        // When author/ has multiple unnamed files, fall back to a strong-signal file outside author/
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "author/first.cpp",
            "author/second.cpp",
            "author.cpp"
        ), Collections.singletonList("cpp"), "task")).contains("author.cpp");

        // Legacy output-only package with root author.txt must still be detected (while ignoring readme.txt)
        assertThat(ProblemPackageResolver.findAuthorSolution(Arrays.asList(
            "author.txt",
            "readme.txt",
            "tests/subtasks.txt"
        ), Collections.singletonList("txt"), "task")).contains("author.txt");
    }
}
