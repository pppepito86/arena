package com.mycompany.myapp.service.dto;

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
            "}";
    }
}
