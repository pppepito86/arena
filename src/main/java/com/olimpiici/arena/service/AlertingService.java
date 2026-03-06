package com.olimpiici.arena.service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.repository.SubmissionRepository;

/**
 * Service for monitoring the system state and sending alerts to administrators.
 * It monitors the submission queue age and identifies problematic submissions.
 */
@Service
public class AlertingService {
    @Autowired
    private MailService mailService;

    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * The recipient email address for alerts.
     */
    private final String mail = "shalamanov.marin+arenaalerts@gmail.com";

    /**
     * Flag to prevent repeated alerts for the same queue delay.
     */
    boolean queueSignaled = false;

    /**
     * Scheduled task to check the submission queue age.
     * <p>
     * Runs every hour. If the oldest submission in the queue is older than 15 minutes,
     * it sends an alert email. The alert is only sent once until the queue is cleared
     * (age falls below 15 minutes).
     */
    @Scheduled(fixedDelay = 1*60*60*1000) // 1 hr
    public void checkQueueAndAlert() {
        List<Submission> submissionsInQueue = submissionRepository.findQueue();

        long ageMins = -1;

        for (int i = submissionsInQueue.size()-1; i >= 0; i--) {
            ZonedDateTime oldestSubmitTime = submissionsInQueue.get(i).getUploadDate();
            if (oldestSubmitTime == null) continue; // Author solutions don't have a timestamp.
            ageMins = (int) ChronoUnit.MINUTES.between(oldestSubmitTime, ZonedDateTime.now());
            break;
        }

        if (ageMins >= 15) {
            if (queueSignaled) {
                // We've already signaled this problem
                return;
            }
            String message = "The queue age is " + ageMins + " and there are " + submissionsInQueue.size() +
                " submissions in the queue.";
            mailService.sendEmail(mail, "Queue age " + ageMins, message, /*multpart*/ false, /*isHtml*/ false);
            queueSignaled = true;
        } else {
            queueSignaled = false;
        }
    }

    /**
     * Scheduled task to check for "bad" submissions (e.g., system errors).
     * <p>
     * Runs once a week. If bad submissions are found, it sends an HTML email
     * containing links to all identified submissions.
     */
    @Scheduled(fixedDelay = 7*24*60*60*1000) // 1 week
    public void checkBadSubmissionsAndAlert() {
        List<Submission> badSubs = submissionRepository.findBadSubmissions();
        if (badSubs.isEmpty()) {
            return;
        }

        String list = badSubs.stream()
            .map(s -> String.format("<a href='https://arena.olimpiici.com/#/submission/%d/view'>%d</a>", s.getId(), s.getId()))
            .collect(Collectors.joining("</br>\n"));
        String message = "<p> There are " + badSubs.size() + " bad submissions. Here's a list of them: </p> \n" + list;

        mailService.sendEmail(mail, badSubs.size() + " bad submissions", message, /*multipart=*/ false, /*isHtml=*/ true);
    }

    /**
     * Sends a custom alert email with the provided content.
     *
     * @param content the content (title) of the alert to send
     */
    public void sendAlert(String content) {
    	mailService.sendEmail(mail, /*title=*/content, /*body=*/"", /*multipart=*/ false, /*isHtml=*/ true);
    }
}
