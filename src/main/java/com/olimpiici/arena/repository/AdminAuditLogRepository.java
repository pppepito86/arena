package com.olimpiici.arena.repository;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.olimpiici.arena.domain.AdminAuditLog;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    /**
     * Find audit entries in a date range, optionally narrowed to one admin
     * and/or one action. A null login or action means "any".
     */
    @Query("select l from AdminAuditLog l where "
        + "(:login is null or l.adminLogin = :login) and "
        + "(:action is null or l.action = :action) and "
        + "l.performedDate >= :from and l.performedDate <= :to")
    Page<AdminAuditLog> find(@Param("login") String login,
                             @Param("action") String action,
                             @Param("from") ZonedDateTime from,
                             @Param("to") ZonedDateTime to,
                             Pageable pageable);

    @Query("select distinct l.action from AdminAuditLog l order by l.action")
    List<String> findDistinctActions();

    @Query("select distinct l.adminLogin from AdminAuditLog l order by l.adminLogin")
    List<String> findDistinctAdminLogins();
}
