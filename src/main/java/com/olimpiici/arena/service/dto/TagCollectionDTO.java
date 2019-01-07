package com.olimpiici.arena.service.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the TagCollection entity.
 */
public class TagCollectionDTO implements Serializable {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TagCollectionDTO tagCollectionDTO = (TagCollectionDTO) o;
        if (tagCollectionDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), tagCollectionDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "TagCollectionDTO{" +
            "id=" + getId() +
            "}";
    }
}
