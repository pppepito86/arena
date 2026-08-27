package com.olimpiici.arena.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One field that an admin changed, as recorded in the {@code details} column
 * of {@code admin_audit_log}.
 */
public class FieldChange {

    private String field;

    @JsonProperty("old")
    private String oldValue;

    @JsonProperty("new")
    private String newValue;

    public FieldChange() {
    }

    public FieldChange(String field, String oldValue, String newValue) {
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    @Override
    public String toString() {
        return field + ": " + oldValue + " -> " + newValue;
    }
}
