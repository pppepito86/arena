package com.olimpiici.arena.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * A TagCollectionTag.
 */
@Entity
@Table(name = "tag_collection_tag")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class TagCollectionTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties("")
    private TagCollection collection;

    @ManyToOne
    @JsonIgnoreProperties("")
    private Tag tag;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TagCollection getCollection() {
        return collection;
    }

    public TagCollectionTag collection(TagCollection tagCollection) {
        this.collection = tagCollection;
        return this;
    }

    public void setCollection(TagCollection tagCollection) {
        this.collection = tagCollection;
    }

    public Tag getTag() {
        return tag;
    }

    public TagCollectionTag tag(Tag tag) {
        this.tag = tag;
        return this;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
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
        TagCollectionTag tagCollectionTag = (TagCollectionTag) o;
        if (tagCollectionTag.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), tagCollectionTag.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "TagCollectionTag{" +
            "id=" + getId() +
            "}";
    }
}
