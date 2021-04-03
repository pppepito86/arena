package com.olimpiici.arena.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "comment")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id")
    private long topicId;

    @Column(name = "content")
    private String content;

    @ManyToOne
    @JoinColumn(name = "author_id")
    @JsonIgnoreProperties("")
    private User author;

    @Column(name = "posted_date")
    private ZonedDateTime postedDate;

    @Column(name = "reviewed")
    private boolean isReviewed;


    @Column(name = "censured")
    private boolean isCensured;

    @ManyToOne
    @JoinColumn(name = "censured_by")
    @JsonIgnoreProperties("")
    private User censuredBy;

    @Column(name = "censured_date")
    private ZonedDateTime censuredDate;

    public Long getId() {
        return id;
    }

    public void setTopicId(long topicId) {
        this.topicId = topicId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public ZonedDateTime getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(ZonedDateTime time) {
        this.postedDate = time;
    }

    public boolean isReviewed() {
        return isReviewed;
    }

    public void setReviewed(boolean isReviewed) {
        this.isReviewed = isReviewed;
    }

    public boolean isCensured() {
        return isCensured;
    }

    public User getCensuredBy() {
        return censuredBy;
    }

    public ZonedDateTime getCensuredDate() {
        return censuredDate;
    }
}
