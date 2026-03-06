package com.olimpiici.arena.service;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.repository.SubmissionRepository;

public class AlertingServiceTest {

    @Mock
    private MailService mailService;

    @Mock
    private SubmissionRepository submissionRepository;

    private AlertingService alertingService;

    private final String alertMail = "shalamanov.marin+arenaalerts@gmail.com";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        alertingService = new AlertingService();
        ReflectionTestUtils.setField(alertingService, "mailService", mailService);
        ReflectionTestUtils.setField(alertingService, "submissionRepository", submissionRepository);
    }

    @Test
    public void testCheckQueueAndAlert_EmptyQueue() {
        when(submissionRepository.findQueue()).thenReturn(Collections.emptyList());

        alertingService.checkQueueAndAlert();

        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    public void testCheckQueueAndAlert_OldQueue() {
        Submission oldSub = new Submission();
        oldSub.setUploadDate(ZonedDateTime.now().minusMinutes(20));
        when(submissionRepository.findQueue()).thenReturn(Arrays.asList(oldSub));

        // First check - should alert
        alertingService.checkQueueAndAlert();
        verify(mailService, times(1)).sendEmail(eq(alertMail), contains("Queue age"), contains("20"), eq(false), eq(false));

        // Second check - should NOT alert again (queueSignaled is true)
        alertingService.checkQueueAndAlert();
        verify(mailService, times(1)).sendEmail(eq(alertMail), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    public void testCheckQueueAndAlert_RecentQueue() {
        Submission recentSub = new Submission();
        recentSub.setUploadDate(ZonedDateTime.now().minusMinutes(5));
        when(submissionRepository.findQueue()).thenReturn(Arrays.asList(recentSub));

        alertingService.checkQueueAndAlert();

        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    public void testCheckQueueAndAlert_ResetSignal() {
        Submission oldSub = new Submission();
        oldSub.setUploadDate(ZonedDateTime.now().minusMinutes(20));
        when(submissionRepository.findQueue()).thenReturn(Arrays.asList(oldSub));

        // Signal triggered
        alertingService.checkQueueAndAlert();
        verify(mailService, times(1)).sendEmail(eq(alertMail), contains("Queue age"), anyString(), eq(false), eq(false));

        // Queue clears
        when(submissionRepository.findQueue()).thenReturn(Collections.emptyList());
        alertingService.checkQueueAndAlert();

        // Queue becomes old again
        when(submissionRepository.findQueue()).thenReturn(Arrays.asList(oldSub));
        alertingService.checkQueueAndAlert();

        // Should alert again
        verify(mailService, times(2)).sendEmail(eq(alertMail), contains("Queue age"), anyString(), eq(false), eq(false));
    }

    @Test
    public void testCheckBadSubmissionsAndAlert_Empty() {
        when(submissionRepository.findBadSubmissions()).thenReturn(Collections.emptyList());

        alertingService.checkBadSubmissionsAndAlert();

        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    public void testCheckBadSubmissionsAndAlert_Found() {
        Submission badSub = new Submission();
        badSub.setId(123L);
        when(submissionRepository.findBadSubmissions()).thenReturn(Arrays.asList(badSub));

        alertingService.checkBadSubmissionsAndAlert();

        verify(mailService).sendEmail(eq(alertMail), contains("1 bad submissions"), contains("submission/123/view"), eq(false), eq(true));
    }

    @Test
    public void testSendAlert() {
        alertingService.sendAlert("Critical Error");

        verify(mailService).sendEmail(eq(alertMail), eq("Critical Error"), eq(""), eq(false), eq(true));
    }
}
