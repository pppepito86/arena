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
    @ManyToOne
    @JsonIgnoreProperties("")
    private CompetitionProblem competitionProblem;
    
    @ManyToOne
    @JsonIgnoreProperties("")
    private Topic topic;

    public Topic getTopic() {
        return topic;
    }
}
