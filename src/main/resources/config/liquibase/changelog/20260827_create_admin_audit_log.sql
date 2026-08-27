--liquibase formatted sql
--changeset shalamanov:20260827

-- Audit trail of actions taken by admins through the web UI. Deliberately has
-- no foreign keys: an audit row has to outlive the user or problem it talks
-- about, and a FK to jhi_user would make UserResource.deleteUser fail for any
-- admin who has ever acted. admin_login and entity_label are snapshots taken
-- at the time of the action, so the log stays readable after a delete
-- or a rename.
create table admin_audit_log (
    id bigint(20) not null auto_increment,
    admin_id bigint(20),
    admin_login varchar(50) not null,
    action varchar(50) not null,
    entity_type varchar(50),
    entity_id bigint(20),
    entity_label varchar(255),
    details text,
    -- Millisecond precision: two actions in the same second still have a
    -- well-defined order in the log.
    performed_date datetime(3) not null,
    primary key (id)
);

CREATE INDEX admin_audit_log_date
ON admin_audit_log (performed_date);

CREATE INDEX admin_audit_log_admin
ON admin_audit_log (admin_login, performed_date);

CREATE INDEX admin_audit_log_entity
ON admin_audit_log (entity_type, entity_id);

--rollback drop table admin_audit_log;
