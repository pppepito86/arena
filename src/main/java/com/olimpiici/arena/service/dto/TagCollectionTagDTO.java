package com.olimpiici.arena.service.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the TagCollectionTag entity.
 */
public class TagCollectionTagDTO implements Serializable {

    private Long id;

    private Long collectionId;

    private Long tagId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(Long tagCollectionId) {
        this.collectionId = tagCollectionId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TagCollectionTagDTO tagCollectionTagDTO = (TagCollectionTagDTO) o;
        if (tagCollectionTagDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), tagCollectionTagDTO.getId());
    }

    
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    
    public String toString() {
        return "TagCollectionTagDTO{" +
            "id=" + getId() +
            ", collection=" + getCollectionId() +
            ", tag=" + getTagId() +
            "}";
    }
}
