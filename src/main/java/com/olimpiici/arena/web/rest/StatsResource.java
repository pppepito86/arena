package com.olimpiici.arena.web.rest;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.dto.Stats;
import com.olimpiici.arena.service.dto.Stats.Metric;
import com.olimpiici.arena.service.dto.Stats.PeriodDelta;
import com.olimpiici.arena.service.dto.Stats.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for getting the audit events.
 */
@RestController
@RequestMapping("/management/stats")
public class StatsResource {
	
	private static final Logger log = LoggerFactory.getLogger(StatsResource.class);

    private final SubmissionRepository submissionRepository;
    
    private final ProblemRepository problemRepository;
    
    private final UserRepository userRepository;

    public StatsResource(SubmissionRepository submissionRepository,
    ProblemRepository problemRepository,
            UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Stats getAll() {
    	Runtime r = Runtime.getRuntime();
    	long memStart = r.totalMemory() - r.freeMemory(); 
    	long timeStart = System.nanoTime();
    	
        Stats stats = new Stats();    
        stats.users = collectUserStats();
        stats.submissions = collectSubmissionsStats();
        stats.problems.total = problemRepository.count();
        stats.problems.submitable = problemRepository.findAll().stream()
            .filter(p -> p.getVersion() != null && p.getVersion() > 0)
            .count();
        stats.queue = collectQueueStats();
        stats.badSubmissionIds = submissionRepository.findBadSubmissions().stream()
            .map(s -> s.getId())
            .collect(Collectors.toList());
        
        long memEnd = r.totalMemory() - r.freeMemory(); 
        long timeEnd = System.nanoTime();
        log.debug("stats request finished in " + (timeEnd - timeStart)/1e9 + " s  "
        		+ (memEnd - memStart) / (1024 * 1024) + " MB mem");
        
        return stats;
    }

    private Metric collectUserStats() {
        Metric m = new Metric();
        m.total = userRepository.count();

        List<PeriodDelta> periods = getPeriodsOfMonths(12);
        List<User> users = userRepository.findAll();
        collectStats(users, u -> u.getCreatedDate(), periods);
        m.periods.add(periods);
        return m;
    }

    private Metric collectSubmissionsStats() {
        Metric m = new Metric();
        m.total = submissionRepository.count();
        List<ZonedDateTime> submissions = submissionRepository.findAllUploadDates(); //Sort.by(Sort.Direction.ASC, "uploadDate"));
        List<PeriodDelta> periods = getPeriodsOfDays(20);
        Function<ZonedDateTime, Instant> getTime = s -> {
            ZonedDateTime uploadDate = s;//(ZonedDateTime) s[0];
            if (uploadDate != null) return uploadDate.toInstant();
            else return null; //ZonedDateTime.now().minus(Period.ofYears(50)).toInstant();
        };

        collectStats(submissions, getTime, periods);
        m.periods.add(periods);

        periods = getPeriodsOfMonths(24);
        collectStats(submissions, getTime, periods);
        m.periods.add(periods);
        return m;
    }

    private Queue collectQueueStats() {
        Queue queue = new Queue();
        List<Submission> submissionsInQueue = submissionRepository.findQueue();
        queue.submissionIds = submissionsInQueue.stream()
            .map(s -> s.getId())
            .collect(Collectors.toList());
        
        queue.ageMins = 0;

        if (!submissionsInQueue.isEmpty()) {
            ZonedDateTime oldestSubmitTime = 
                submissionsInQueue.get(submissionsInQueue.size()-1).getUploadDate();
            if (oldestSubmitTime == null) {
                queue.ageMins = -1;    
            } else {
                queue.ageMins =
                    (int) ChronoUnit.MINUTES.between(oldestSubmitTime, ZonedDateTime.now());
            }
        }
         
        return queue;
    }

    private List<PeriodDelta> getPeriodsOfMonths(int last) {
        List<PeriodDelta> periods = new ArrayList<>();
        ZonedDateTime beginingOfMonth = ZonedDateTime.now()
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault());

        for (int i = 0; i <= last; i++) {
            ZonedDateTime beginning = beginingOfMonth.minus(Period.ofMonths(i));
            periods.add(new PeriodDelta(beginning.getMonth().name().substring(0, 3), beginning));
        }
        return periods;
    }

    private List<PeriodDelta> getPeriodsOfDays(int last) {
        List<PeriodDelta> periods = new ArrayList<>();
        ZonedDateTime beginingOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);

        for (int i = 0; i <= last; i++) {
            ZonedDateTime beginning = beginingOfDay.minus(Period.ofDays(i));
            String name = beginning.getDayOfMonth() + " " + beginning.getMonth().name().substring(0, 3);
            periods.add(new PeriodDelta(name, beginning));
        }
        return periods;
    }

    private static <T> void collectStats(List<T> entities, Function<T, Instant> getTime, List<PeriodDelta> periods) {
        PeriodDelta currPeriod = periods.get(0);
        int currPeriodIdx = 0;
        
        out: 
        for (int i = entities.size() - 1; i >= 0; i--) {
            T entity = entities.get(i);
            Instant time = getTime.apply(entity);
            if (time == null) {
                continue;
            }
            while (time.isBefore(currPeriod.timestamp.toInstant())) {
                currPeriodIdx++;
                if (currPeriodIdx >= periods.size()) {
                    break out;
                }
                currPeriod = periods.get(currPeriodIdx);
            }
            currPeriod.delta++;
        }
    }
}
