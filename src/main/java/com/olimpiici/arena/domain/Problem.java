package com.olimpiici.arena.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * A Problem.
 */
@Entity
@Table(name = "problem")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Problem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "jhi_directory")
    private String directory;

    @Column(name = "version")
    private Integer version;

    @Column(name = "year")
    private Integer year;

    @Column(name = "group_name")
    private String group;

    @ManyToOne
    @JsonIgnoreProperties("")
    private Competition competition;

    @ManyToOne
    @JsonIgnoreProperties("")
    private TagCollection tags;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public Problem title(String title) {
        this.title = title;
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDirectory() {
        return directory;
    }

    public Problem directory(String directory) {
        this.directory = directory;
        return this;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Integer getVersion() {
        return version;
    }

    public Problem version(Integer version) {
        this.version = version;
        return this;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public TagCollection getTags() {
        return tags;
    }

    public Problem tags(TagCollection tagCollection) {
        this.tags = tagCollection;
        return this;
    }

    public void setTags(TagCollection tagCollection) {
        this.tags = tagCollection;
    }
   
    public String getGroup() {
        return group;
    }

    public Problem group(String group) {
        this.group = group;
        return this;
    }

    public void setGroup(String group) {
        this.group = group;
    }
    
    public Integer getYear() {
        return year;
    }

    public Problem group(Integer year) {
        this.year = year;
        return this;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Competition getCompetition() {
        return competition;
    }

    public Problem competition(Competition competition) {
        this.competition = competition;
        return this;
    }

    public void setCompetition(Competition competition) {
        this.competition = competition;
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
        Problem problem = (Problem) o;
        if (problem.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), problem.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Problem{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", directory='" + getDirectory() + "'" +
            ", version=" + getVersion() +
            ", year=" + getYear() + 
            ", competition=" + getCompetition() ? getCompetition().getId() : "" + 
            ", group=" + getGroup() +
            "}";
    }
}
