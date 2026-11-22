package com.olimpiici.arena.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic summary of a problem's uploaded ZIP package, showing what Arena
 * and the grading worker detected (limits, statement, author solution, tests,
 * checker/interactive components, and any warnings).
 */
public class ProblemPackageInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean hasPackage;
    private String propertiesPath;
    private Integer timeMs;
    private Integer memoryMb;
    private String extension;
    private List<String> allowedExtensions = new ArrayList<>();
    private String statementPath;
    private String authorSolutionPath;
    private String checkerPath;
    private String graderPath;
    private String managerPath;
    private int testCount;
    private int groupCount;
    private boolean testsCleanedUp;
    private List<String> warnings = new ArrayList<>();

    public boolean isHasPackage() {
        return hasPackage;
    }

    public void setHasPackage(boolean hasPackage) {
        this.hasPackage = hasPackage;
    }

    public String getPropertiesPath() {
        return propertiesPath;
    }

    public void setPropertiesPath(String propertiesPath) {
        this.propertiesPath = propertiesPath;
    }

    public Integer getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(Integer timeMs) {
        this.timeMs = timeMs;
    }

    public Integer getMemoryMb() {
        return memoryMb;
    }

    public void setMemoryMb(Integer memoryMb) {
        this.memoryMb = memoryMb;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public String getStatementPath() {
        return statementPath;
    }

    public void setStatementPath(String statementPath) {
        this.statementPath = statementPath;
    }

    public String getAuthorSolutionPath() {
        return authorSolutionPath;
    }

    public void setAuthorSolutionPath(String authorSolutionPath) {
        this.authorSolutionPath = authorSolutionPath;
    }

    public String getCheckerPath() {
        return checkerPath;
    }

    public void setCheckerPath(String checkerPath) {
        this.checkerPath = checkerPath;
    }

    public String getGraderPath() {
        return graderPath;
    }

    public void setGraderPath(String graderPath) {
        this.graderPath = graderPath;
    }

    public String getManagerPath() {
        return managerPath;
    }

    public void setManagerPath(String managerPath) {
        this.managerPath = managerPath;
    }

    public int getTestCount() {
        return testCount;
    }

    public void setTestCount(int testCount) {
        this.testCount = testCount;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }

    public boolean isTestsCleanedUp() {
        return testsCleanedUp;
    }

    public void setTestsCleanedUp(boolean testsCleanedUp) {
        this.testsCleanedUp = testsCleanedUp;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
