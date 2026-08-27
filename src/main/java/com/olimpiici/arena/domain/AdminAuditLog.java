package com.olimpiici.arena.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * A single action taken by an admin through the web UI.
 * <p>
 * Distinct from {@link PersistentAuditEvent}, which the Spring Boot actuator
 * fills with authentication events only.
 * <p>
 * The table has no foreign keys on purpose (see the liquibase changelog):
 * an audit row must outlive the user or entity it describes, so
 * {@code adminId}/{@code entityId} are loose references and
 * {@code adminLogin}/{@code entityLabel} are snapshots taken when the action
 * happened.
 */
@Entity
@Table(name = "admin_audit_log")
public class AdminAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_login")
    private String adminLogin;

    @Column(name = "action")
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_label")
    private String entityLabel;

    /** JSON produced by AdminAuditService, e.g. {"changes":[{"field":"time","old":"1000","new":"2000"}]}. */
    @Column(name = "details")
    private String details;

    @Column(name = "performed_date")
    private ZonedDateTime performedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getAdminLogin() {
        return adminLogin;
    }

    public void setAdminLogin(String adminLogin) {
        this.adminLogin = adminLogin;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getEntityLabel() {
        return entityLabel;
    }

    public void setEntityLabel(String entityLabel) {
        this.entityLabel = entityLabel;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ZonedDateTime getPerformedDate() {
        return performedDate;
    }

    public void setPerformedDate(ZonedDateTime performedDate) {
        this.performedDate = performedDate;
    }

    @Override
    public String toString() {
        return "AdminAuditLog{" +
            "id=" + id +
            ", adminLogin='" + adminLogin + "'" +
            ", action='" + action + "'" +
            ", entityType='" + entityType + "'" +
            ", entityId=" + entityId +
            ", details='" + details + "'" +
            ", performedDate='" + performedDate + "'" +
            "}";
    }
}
