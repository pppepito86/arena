package com.olimpiici.arena.service.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the Problem entity.
 */
public class ProblemDTO implements Serializable {

    private Long id;

    private String title;

    private String directory;

    private Integer version;

    private Integer time;
    
    private Integer memory;
    
    private Long tagsId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getTime() {
		return time;
	}
    
    public void setTime(Integer time) {
		this.time = time;
	}
    
    public Integer getMemory() {
		return memory;
	}
    
    public void setMemory(Integer memory) {
		this.memory = memory;
	}
    
    public Long getTagsId() {
        return tagsId;
    }

    public void setTagsId(Long tagCollectionId) {
        this.tagsId = tagCollectionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProblemDTO problemDTO = (ProblemDTO) o;
        if (problemDTO.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), problemDTO.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "ProblemDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", directory='" + getDirectory() + "'" +
            ", version=" + getVersion() +
            ", tags=" + getTagsId() +
            "}";
    }
}
