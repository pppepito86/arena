package com.olimpiici.arena.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "problem_topic")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ProblemTopic implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private long competitionProblemId;

    @ManyToOne
    @JsonIgnoreProperties("")
    private Topic topic;

    public Topic getTopic() {
        return topic;
    }

    public void setCompetitionProblem(long cp)  {
        this.competitionProblemId = cp;
    }

    public void setTopic(Topic t)  {
        this.topic = t;
    }
}
