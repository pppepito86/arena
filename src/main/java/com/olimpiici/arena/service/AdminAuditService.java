package com.olimpiici.arena.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.olimpiici.arena.domain.AdminAuditLog;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.AdminAuditLogRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.security.SecurityUtils;
import com.olimpiici.arena.service.dto.FieldChange;

/**
 * Records what admins do, and is the only place that writes to
 * {@code admin_audit_log}.
 * <p>
 * Every method swallows its own failures: an audit entry must never be the
 * reason an admin action fails. The methods join the caller's transaction, so
 * an action that rolls back leaves no row claiming it happened.
 */
@Service
@Transactional
public class AdminAuditService {

    /** Recorded as the actor when there is no authenticated user (scheduled jobs). */
    public static final String SYSTEM_LOGIN = "system";

    public static final String ENTITY_PROBLEM = "problem";
    public static final String ENTITY_USER = "user";

    private final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

    private final AdminAuditLogRepository adminAuditLogRepository;

    private final UserRepository userRepository;

    /**
     * Deliberately not the application's shared ObjectMapper: the shape of a
     * stored audit entry should not change if the app's Jackson configuration
     * ever does.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminAuditService(AdminAuditLogRepository adminAuditLogRepository,
            UserRepository userRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Record an action that has no before/after values, such as creating a problem.
     */
    public void log(String action, String entityType, Long entityId, String entityLabel) {
        log(action, entityType, entityId, entityLabel, null, Collections.emptyList());
    }

    /**
     * Record an action together with the fields it changed.
     */
    public void log(String action, String entityType, Long entityId, String entityLabel,
            List<FieldChange> changes) {
        log(action, entityType, entityId, entityLabel, null, changes);
    }

    /**
     * Record an action, the fields it changed and a free-text note (a file name, say).
     */
    public void log(String action, String entityType, Long entityId, String entityLabel,
            String note, List<FieldChange> changes) {
        try {
            AdminAuditLog entry = new AdminAuditLog();
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setEntityLabel(entityLabel);
            entry.setDetails(toJson(note, changes));
            entry.setPerformedDate(ZonedDateTime.now());

            String login = SecurityUtils.getCurrentUserLogin().orElse(SYSTEM_LOGIN);
            entry.setAdminLogin(login);
            entry.setAdminId(userRepository.findOneByLogin(login)
                .map(User::getId)
                .orElse(null));

            adminAuditLogRepository.save(entry);
            log.debug("Audited admin action: {}", entry);
        } catch (Exception e) {
            // An admin action must not fail because we could not audit it.
            log.error("Cannot write admin audit entry for action " + action
                + " on " + entityType + " " + entityId, e);
        }
    }

    /**
     * Record an action only if something actually changed. Mirrors
     * ProblemService.updateTimeLimit/updateMemoryLimit, which return early when
     * the new value equals the old one, so a no-op save writes no audit row.
     */
    public void logIfChanged(String action, String entityType, Long entityId, String entityLabel,
            List<FieldChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        log(action, entityType, entityId, entityLabel, null, changes);
    }

    /**
     * A single changed field, or null when the value did not change (so it can be
     * dropped by {@link #changes(FieldChange...)}).
     */
    public static FieldChange change(String field, Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return null;
        }
        return new FieldChange(field, asString(oldValue), asString(newValue));
    }

    /**
     * Collect the changes that really happened, dropping the nulls that
     * {@link #change(String, Object, Object)} returns for unchanged fields.
     */
    public static List<FieldChange> changes(FieldChange... maybeNull) {
        List<FieldChange> result = new ArrayList<>();
        Arrays.stream(maybeNull)
            .filter(Objects::nonNull)
            .forEach(result::add);
        return result;
    }

    private String toJson(String note, List<FieldChange> changes) throws Exception {
        Map<String, Object> details = new LinkedHashMap<>();
        if (note != null) {
            details.put("note", note);
        }
        if (changes != null && !changes.isEmpty()) {
            details.put("changes", changes);
        }
        if (details.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(details);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
