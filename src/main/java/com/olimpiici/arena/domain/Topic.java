package com.olimpiici.arena.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "topic")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Topic implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    @JsonIgnoreProperties("")
    private User creator;
    
    @Column(name = "created_date")
    private ZonedDateTime createdDate;

    @OneToMany(mappedBy = "topicId")
    private List<Comment> comments;

    @OneToMany(mappedBy = "topic")
    @JsonIgnoreProperties("")
    private List<TopicSubscriptions> subscriptions;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public ZonedDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(ZonedDateTime date) {
        createdDate = date;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<TopicSubscriptions> getSubscriptions() {
        return subscriptions;
    }
}
