package com.mycompany.myapp.service.dto;

import java.time.ZonedDateTime;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the Submission entity.
 */
public class SubmissionDTO implements Serializable {

    private Long id;

    private String file;

    private String verdict;

    private String details;

    private Integer points;

    private Integer timeInMillis;

    private Integer memoryInBytes;

    private ZonedDateTime uploadDate;

    private Long userId;

    private Long competitionProblemId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getTimeInMillis() {
        return timeInMillis;
    }

    public void setTimeInMillis(Integer timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    public Integer getMemoryInBytes() {
        return memoryInBytes;
    }

    public void setMemoryInBytes(Integer memoryInBytes) {
        this.memoryInBytes = memoryInBytes;
    }

    public ZonedDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(ZonedDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCompetitionProblemId() {
        return competitionProblemId;
    }

    public void setCompetitionProblemId(Long competitionProblemId) {
        this.competitionProblemId = competitionProblemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SubmissionDTO submissionDTO = (SubmissionDTO) o;
        if (submissionDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), submissionDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "SubmissionDTO{" +
            "id=" + getId() +
            ", file='" + getFile() + "'" +
            ", verdict='" + getVerdict() + "'" +
            ", details='" + getDetails() + "'" +
            ", points=" + getPoints() +
            ", timeInMillis=" + getTimeInMillis() +
            ", memoryInBytes=" + getMemoryInBytes() +
            ", uploadDate='" + getUploadDate() + "'" +
            ", user=" + getUserId() +
            ", competitionProblem=" + getCompetitionProblemId() +
            "}";
    }
}
