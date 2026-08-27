package com.olimpiici.arena.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.olimpiici.arena.domain.AdminAuditLog;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.AdminAuditLogRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.dto.FieldChange;

public class AdminAuditServiceTest {

    @Mock
    private AdminAuditLogRepository adminAuditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Captor
    private ArgumentCaptor<AdminAuditLog> entryCaptor;

    private AdminAuditService adminAuditService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        adminAuditService = new AdminAuditService(adminAuditLogRepository, userRepository);
        loginAs("marin", 7L);
    }

    @After
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String login, Long id) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(login, login));
        User user = new User();
        user.setId(id);
        user.setLogin(login);
        when(userRepository.findOneByLogin(login)).thenReturn(Optional.of(user));
    }

    @Test
    public void logsTheActorAndTheEntity() {
        adminAuditService.log("problem.create", AdminAuditService.ENTITY_PROBLEM, 42L, "Sum of two numbers");

        verify(adminAuditLogRepository).save(entryCaptor.capture());
        AdminAuditLog entry = entryCaptor.getValue();
        assertThat(entry.getAdminLogin()).isEqualTo("marin");
        assertThat(entry.getAdminId()).isEqualTo(7L);
        assertThat(entry.getAction()).isEqualTo("problem.create");
        assertThat(entry.getEntityType()).isEqualTo("problem");
        assertThat(entry.getEntityId()).isEqualTo(42L);
        assertThat(entry.getEntityLabel()).isEqualTo("Sum of two numbers");
        assertThat(entry.getPerformedDate()).isNotNull();
        // Nothing to say about an action with no before/after values.
        assertThat(entry.getDetails()).isNull();
    }

    @Test
    public void serializesChangedFieldsAsJson() {
        List<FieldChange> changes = AdminAuditService.changes(
            AdminAuditService.change("time", 1000, 2000),
            AdminAuditService.change("memory", 256, 256));

        adminAuditService.log("problem.update", AdminAuditService.ENTITY_PROBLEM, 42L, "Sum", changes);

        verify(adminAuditLogRepository).save(entryCaptor.capture());
        // memory was unchanged, so it is not in the log at all.
        assertThat(entryCaptor.getValue().getDetails())
            .isEqualTo("{\"changes\":[{\"field\":\"time\",\"old\":\"1000\",\"new\":\"2000\"}]}");
    }

    @Test
    public void serializesTheNoteAlongsideTheChanges() {
        adminAuditService.log("problem.upload_zip", AdminAuditService.ENTITY_PROBLEM, 42L, "Sum",
            "task.zip (2048 bytes)",
            AdminAuditService.changes(AdminAuditService.change("memory", 256, 512)));

        verify(adminAuditLogRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getDetails()).isEqualTo(
            "{\"note\":\"task.zip (2048 bytes)\",\"changes\":"
            + "[{\"field\":\"memory\",\"old\":\"256\",\"new\":\"512\"}]}");
    }

    @Test
    public void changeIsNullWhenNothingChanged() {
        assertThat(AdminAuditService.change("time", 1000, 1000)).isNull();
        assertThat(AdminAuditService.change("title", null, null)).isNull();
        assertThat(AdminAuditService.change("title", "Sum", "Sum of two numbers")).isNotNull();
        // A field that was empty and now has a value is a change worth recording.
        FieldChange filledIn = AdminAuditService.change("directory", null, "sum");
        assertThat(filledIn.getOldValue()).isNull();
        assertThat(filledIn.getNewValue()).isEqualTo("sum");
    }

    @Test
    public void changesDropsTheUnchangedFields() {
        List<FieldChange> changes = AdminAuditService.changes(
            AdminAuditService.change("time", 1000, 1000),
            AdminAuditService.change("memory", 256, 512),
            null);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getField()).isEqualTo("memory");
    }

    @Test
    public void logIfChangedWritesNothingForAnEmptyDiff() {
        adminAuditService.logIfChanged("problem.update", AdminAuditService.ENTITY_PROBLEM, 42L, "Sum",
            Collections.emptyList());
        adminAuditService.logIfChanged("problem.update", AdminAuditService.ENTITY_PROBLEM, 42L, "Sum", null);

        verify(adminAuditLogRepository, never()).save(any(AdminAuditLog.class));
    }

    @Test
    public void recordsSystemWhenNobodyIsLoggedIn() {
        SecurityContextHolder.clearContext();

        adminAuditService.log("problem.limits.auto_set", AdminAuditService.ENTITY_PROBLEM, 42L, "Sum");

        verify(adminAuditLogRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getAdminLogin()).isEqualTo(AdminAuditService.SYSTEM_LOGIN);
        assertThat(entryCaptor.getValue().getAdminId()).isNull();
    }

    @Test
    public void keepsTheLoginEvenWhenTheUserRowIsGone() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("ghost", "ghost"));
        when(userRepository.findOneByLogin("ghost")).thenReturn(Optional.empty());

        adminAuditService.log("user.deactivate", AdminAuditService.ENTITY_USER, 9L, "someone");

        verify(adminAuditLogRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getAdminLogin()).isEqualTo("ghost");
        assertThat(entryCaptor.getValue().getAdminId()).isNull();
    }

    @Test
    public void aFailedAuditWriteDoesNotBreakTheAdminAction() {
        doThrow(new RuntimeException("db is down"))
            .when(adminAuditLogRepository).save(any(AdminAuditLog.class));

        // No exception: the admin's action has already succeeded by this point.
        adminAuditService.log("user.activate", AdminAuditService.ENTITY_USER, 9L, "someone");

        verify(adminAuditLogRepository).save(any(AdminAuditLog.class));
    }
}
