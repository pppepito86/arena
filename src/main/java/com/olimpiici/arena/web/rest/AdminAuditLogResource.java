package com.olimpiici.arena.web.rest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.olimpiici.arena.domain.AdminAuditLog;
import com.olimpiici.arena.repository.AdminAuditLogRepository;
import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.web.rest.util.PaginationUtil;

/**
 * REST controller for reading the admin audit log.
 * <p>
 * Lives under /api rather than /management because this is application data,
 * not actuator data (see AuditResource for the latter).
 */
@RestController
@RequestMapping("/api")
public class AdminAuditLogResource {

    private static final String RESOURCE_URL = "/api/admin-audit-log";

    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminAuditLogResource(AdminAuditLogRepository adminAuditLogRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    /**
     * GET /admin-audit-log : get a page of admin actions, newest first.
     *
     * @param fromDate only entries on or after this day (defaults to a month ago)
     * @param toDate only entries on or before this day (defaults to today)
     * @param adminLogin only entries by this admin, or all admins when absent
     * @param action only entries with this action, or all actions when absent
     * @param pageable the pagination information
     */
    @GetMapping("/admin-audit-log")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<AdminAuditLog>> getAll(
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "adminLogin", required = false) String adminLogin,
            @RequestParam(value = "action", required = false) String action,
            Pageable pageable) {

        ZoneId zone = ZoneId.systemDefault();
        LocalDate to = toDate != null ? toDate : LocalDate.now(zone);
        LocalDate from = fromDate != null ? fromDate : to.minusMonths(1);

        ZonedDateTime fromTime = from.atStartOfDay(zone);
        // toDate is inclusive: everything up to the end of that day.
        ZonedDateTime toTime = to.plusDays(1).atStartOfDay(zone);

        Page<AdminAuditLog> page = adminAuditLogRepository.find(
            emptyToNull(adminLogin), emptyToNull(action), fromTime, toTime, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, RESOURCE_URL);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET /admin-audit-log/actions : the action names present in the log, for
     * the filter dropdown.
     */
    @GetMapping("/admin-audit-log/actions")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public List<String> getActions() {
        return adminAuditLogRepository.findDistinctActions();
    }

    /**
     * GET /admin-audit-log/admins : the admins present in the log, for the
     * filter dropdown.
     */
    @GetMapping("/admin-audit-log/admins")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public List<String> getAdmins() {
        return adminAuditLogRepository.findDistinctAdminLogins();
    }

    private static String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }
}
