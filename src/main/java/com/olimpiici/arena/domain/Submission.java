package com.olimpiici.arena.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A Submission.
 */
@Entity
@Table(name = "submission")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Submission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jhi_file")
    private String file;

    @Column(name = "verdict")
    private String verdict;

    @Column(name = "details")
    private String details;

    @Column(name = "points")
    private Integer points;

    @Column(name = "time_in_millis")
    private Integer timeInMillis;

    @Column(name = "memory_in_bytes")
    private Integer memoryInBytes;

    @Column(name = "upload_date")
    private ZonedDateTime uploadDate;

    @Column(name = "security_key")
    private String securityKey;

    @ManyToOne
    @JsonIgnoreProperties("")
    private TagCollection tags;

    @ManyToOne
    @JsonIgnoreProperties("")
    private User user;

    @ManyToOne
    @JsonIgnoreProperties("")
    private CompetitionProblem competitionProblem;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFile() {
        return file;
    }

    public Submission file(String file) {
        this.file = file;
        return this;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getVerdict() {
        return verdict;
    }

    public Submission verdict(String verdict) {
        this.verdict = verdict;
        return this;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getDetails() {
        return details;
    }

    public Submission details(String details) {
        this.details = details;
        return this;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Integer getPoints() {
        return points;
    }

    public Submission points(Integer points) {
        this.points = points;
        return this;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Integer getTimeInMillis() {
        return timeInMillis;
    }

    public Submission timeInMillis(Integer timeInMillis) {
        this.timeInMillis = timeInMillis;
        return this;
    }

    public void setTimeInMillis(Integer timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    public Integer getMemoryInBytes() {
        return memoryInBytes;
    }

    public Submission memoryInBytes(Integer memoryInBytes) {
        this.memoryInBytes = memoryInBytes;
        return this;
    }

    public void setMemoryInBytes(Integer memoryInBytes) {
        this.memoryInBytes = memoryInBytes;
    }

    public ZonedDateTime getUploadDate() {
        return uploadDate;
    }

    public Submission uploadDate(ZonedDateTime uploadDate) {
        this.uploadDate = uploadDate;
        return this;
    }

    public void setUploadDate(ZonedDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getSecurityKey() {
        return securityKey;
    }

    public Submission securityKey(String securityKey) {
        this.securityKey = securityKey;
        return this;
    }

    public void setSecurityKey(String securityKey) {
        this.securityKey = securityKey;
    }

    public TagCollection getTags() {
        return tags;
    }

    public Submission tags(TagCollection tagCollection) {
        this.tags = tagCollection;
        return this;
    }

    public void setTags(TagCollection tagCollection) {
        this.tags = tagCollection;
    }

    public User getUser() {
        return user;
    }

    public Submission user(User user) {
        this.user = user;
        return this;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public CompetitionProblem getCompetitionProblem() {
        return competitionProblem;
    }

    public Submission competitionProblem(CompetitionProblem competitionProblem) {
        this.competitionProblem = competitionProblem;
        return this;
    }

    public void setCompetitionProblem(CompetitionProblem competitionProblem) {
        this.competitionProblem = competitionProblem;
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
        Submission submission = (Submission) o;
        if (submission.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), submission.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Submission{" +
            "id=" + getId() +
            ", file='" + getFile() + "'" +
            ", verdict='" + getVerdict() + "'" +
            ", details='" + getDetails() + "'" +
            ", points=" + getPoints() +
            ", timeInMillis=" + getTimeInMillis() +
            ", memoryInBytes=" + getMemoryInBytes() +
            ", uploadDate='" + getUploadDate() + "'" +
            ", securityKey='" + getSecurityKey() + "'" +
            "}";
    }
}
