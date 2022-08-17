package com.olimpiici.arena.service.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * A DTO for the CompetitionProblem entity.
 */
public class CompetitionProblemDTO implements Serializable {

    private Long id;

    private Integer order;

    private Long problemId;

    private Long competitionId;

    private String title;

    private Integer points;

    public List<String> path;

    public Integer getPoints() {
		return points;
	}

	public void setPoints(Integer points) {
		this.points = points;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Long getProblemId() {
        return problemId;
    }

    public void setProblemId(Long problemId) {
        this.problemId = problemId;
    }

    public Long getCompetitionId() {
        return competitionId;
    }

    public void setCompetitionId(Long competitionId) {
        this.competitionId = competitionId;
    }


    @Override
	public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompetitionProblemDTO competitionProblemDTO = (CompetitionProblemDTO) o;
        if (competitionProblemDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), competitionProblemDTO.getId());
    }


    @Override
	public int hashCode() {
        return Objects.hashCode(getId());
    }


    @Override
	public String toString() {
        return "CompetitionProblemDTO{" +
            "id=" + getId() +
            ", order=" + getOrder() +
            ", problem=" + getProblemId() +
            ", competition=" + getCompetitionId() +
            "}";
    }
}
