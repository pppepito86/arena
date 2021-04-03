package com.olimpiici.arena.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A CompetitionProblem.
 */
@Entity
@Table(name = "competition_problem")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class CompetitionProblem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jhi_order")
    private Integer order;

    @ManyToOne
    @JsonIgnoreProperties("")
    private Problem problem;

    @ManyToOne
    @JsonIgnoreProperties("")
    private Competition competition;

    @OneToMany(mappedBy = "competitionProblemId")
    @JsonIgnoreProperties("")
    private List<ProblemTopic> problemTopic;
    
    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getOrder() {
        return order;
    }

    public CompetitionProblem order(Integer order) {
        this.order = order;
        return this;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Problem getProblem() {
        return problem;
    }

    public CompetitionProblem problem(Problem problem) {
        this.problem = problem;
        return this;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public Competition getCompetition() {
        return competition;
    }

    public CompetitionProblem competition(Competition competition) {
        this.competition = competition;
        return this;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

    public Topic getTopic() {
        if (problemTopic == null || problemTopic.isEmpty()) {
            return null;
        }
        
        return problemTopic.get(0).getTopic();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompetitionProblem competitionProblem = (CompetitionProblem) o;
        if (competitionProblem.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), competitionProblem.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "CompetitionProblem{" +
            "id=" + getId() +
            ", order=" + getOrder() +
            "}";
    }
}
