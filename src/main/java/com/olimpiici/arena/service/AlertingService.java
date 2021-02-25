package com.olimpiici.arena.service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.repository.SubmissionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
public class AlertingService {
    @Autowired
    private MailService mailService;
    
    @Autowired
    private SubmissionRepository submissionRepository;

    private final String mail = "shalamanov.marin+arenaalerts@gmail.com";

    boolean queueSignaled = false;

    @Scheduled(fixedDelay = 1*60*60*1000) // 1 hr
    public void checkQeueueAndAlert() {
        List<Submission> submissionsInQueue = submissionRepository.findQueue();
        
        long ageMins = 0;
        if (!submissionsInQueue.isEmpty()) {
            ZonedDateTime oldestSubmitTime = 
                submissionsInQueue.get(submissionsInQueue.size()-1).getUploadDate();
            ageMins = (int) ChronoUnit.MINUTES.between(oldestSubmitTime, ZonedDateTime.now());
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

    @Scheduled(fixedDelay = 7*24*60*60*1000) // 1 week
    public void checkBadSubmissionsAndAlert() {
        List<Submission> badSubs = submissionRepository.findBadSubmissions();
        if (badSubs.isEmpty()) {
            return;
        }

        String list = badSubs.stream()
            .map(s -> String.format("<a href='https://arena.olimpiici.com/#/submission/%d/view'>%d</a>", s.getId(), s.getId()))
            .collect(Collectors.joining("\n"));
        String message = "There are " + badSubs.size() + " bad submissions. Here's a list of them: \n" + list;

        mailService.sendEmail(mail, badSubs.size() + " bad submissions", message, /*multpart*/ false, /*isHtml*/ false);   
    }
}
