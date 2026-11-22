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

}
