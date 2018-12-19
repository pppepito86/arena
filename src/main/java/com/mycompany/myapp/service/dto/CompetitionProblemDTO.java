package com.mycompany.myapp.service.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the CompetitionProblem entity.
 */
public class CompetitionProblemDTO implements Serializable {

    private Long id;

    private Integer order;

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
            "}";
    }
}
