package com.olimpiici.arena.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "topic_subscriptions")
@IdClass(TopicSubscriptions.class)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TopicSubscriptions implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne
    @JsonIgnoreProperties("")
    private User user;
    
    @Id
    @ManyToOne
    @JsonIgnoreProperties("")
    private Topic topic;
}
