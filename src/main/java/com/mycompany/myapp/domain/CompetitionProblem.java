package com.mycompany.myapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

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

    @OneToMany(mappedBy = "competitionProblem")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Problem> problems = new HashSet<>();
    @OneToMany(mappedBy = "competitionProblem")
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    private Set<Competition> competitions = new HashSet<>();
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

    public Set<Problem> getProblems() {
        return problems;
    }

    public CompetitionProblem problems(Set<Problem> problems) {
        this.problems = problems;
        return this;
    }

    public CompetitionProblem addProblem(Problem problem) {
        this.problems.add(problem);
        problem.setCompetitionProblem(this);
        return this;
    }

    public CompetitionProblem removeProblem(Problem problem) {
        this.problems.remove(problem);
        problem.setCompetitionProblem(null);
        return this;
    }

    public void setProblems(Set<Problem> problems) {
        this.problems = problems;
    }

    public Set<Competition> getCompetitions() {
        return competitions;
    }

    public CompetitionProblem competitions(Set<Competition> competitions) {
        this.competitions = competitions;
        return this;
    }

    public CompetitionProblem addCompetition(Competition competition) {
        this.competitions.add(competition);
        competition.setCompetitionProblem(this);
        return this;
    }

    public CompetitionProblem removeCompetition(Competition competition) {
        this.competitions.remove(competition);
        competition.setCompetitionProblem(null);
        return this;
    }

    public void setCompetitions(Set<Competition> competitions) {
        this.competitions = competitions;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

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
