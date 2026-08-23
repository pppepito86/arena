package com.olimpiici.arena.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Expected grading outcome for a solution file, deserialized from the
 * "<solutionName>.json" file sitting next to a solution source file
 * (e.g. "correct.json" for "correct.cpp") under
 * src/test/resources/integration-test/{problem}/solutions/.
 *
 * All fields are optional; only the ones present are asserted.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolutionExpectation {

    private String verdict;
    private String verdictContains;
    private Integer points;
    private Integer pointsLessThan;

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getVerdictContains() {
        return verdictContains;
    }

    public void setVerdictContains(String verdictContains) {
        this.verdictContains = verdictContains;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getPointsLessThan() {
        return pointsLessThan;
    }

    public void setPointsLessThan(Integer pointsLessThan) {
        this.pointsLessThan = pointsLessThan;
    }
}
