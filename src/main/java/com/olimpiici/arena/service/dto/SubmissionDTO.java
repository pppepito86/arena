package com.olimpiici.arena.service.dto;

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

    private String securityKey;

    private Long tagsId;

    private Long userId;

    private Long competitionProblemId;

    private String code;
    
    private String userFirstName;

    private String userLastName;
    
    private String problemName;
    
    private String competitionName;
    
    private Long competitionId;

	public String getUserFirstName() {
		return userFirstName;
	}

	public void setUserFirstName(String userFirstName) {
		this.userFirstName = userFirstName;
	}

	public String getUserLastName() {
		return userLastName;
	}

	public void setUserLastName(String userLastName) {
		this.userLastName = userLastName;
	}

	public String getProblemName() {
		return problemName;
	}

	public void setProblemName(String problemName) {
		this.problemName = problemName;
	}

	public String getCompetitionName() {
		return competitionName;
	}

	public void setCompetitionName(String competitionName) {
		this.competitionName = competitionName;
	}

	public Long getCompetitionId() {
		return competitionId;
	}

	public void setCompetitionId(Long competitionId) {
		this.competitionId = competitionId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

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

    public String getSecurityKey() {
        return securityKey;
    }

    public void setSecurityKey(String securityKey) {
        this.securityKey = securityKey;
    }

    public Long getTagsId() {
        return tagsId;
    }

    public void setTagsId(Long tagCollectionId) {
        this.tagsId = tagCollectionId;
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

    
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    
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
            ", securityKey='" + getSecurityKey() + "'" +
            ", tags=" + getTagsId() +
            ", user=" + getUserId() +
            ", competitionProblem=" + getCompetitionProblemId() +
            "}";
    }
}
