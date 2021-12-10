package com.olimpiici.arena.web.rest;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.dto.Stats;
import com.olimpiici.arena.service.dto.Stats.Metric;
import com.olimpiici.arena.service.dto.Stats.PeriodDelta;
import com.olimpiici.arena.service.dto.Stats.Queue;

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
        stats.activeUsers = collectActiveUserStats();
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

    private Metric collectActiveUserStats() {
    	Metric m = new Metric();

    	BiFunction<ZonedDateTime, ZonedDateTime, Integer> function =
    			(ZonedDateTime from, ZonedDateTime to) ->
    					submissionRepository.findNumSubmitters(from, to);

    	m.periods.add(collectStats(function, getPeriodsOfMonths(24)));
    	m.periods.add(collectStats(function, getPeriodsOfDays(20)));

    	return m;
    }

    private Metric collectSubmissionsStats() {
        Metric m = new Metric();
        m.total = submissionRepository.count();
        List<ZonedDateTime> submissions = submissionRepository.findAllUploadDates(); //Sort.by(Sort.Direction.ASC, "uploadDate"));

        Function<ZonedDateTime, Instant> getTime = s -> {
            ZonedDateTime uploadDate = s;//(ZonedDateTime) s[0];
            if (uploadDate != null) return uploadDate.toInstant();
            else return null; //ZonedDateTime.now().minus(Period.ofYears(50)).toInstant();
        };

        m.periods.add(
        		collectStats(submissions, getTime, getPeriodsOfDays(20)));

        m.periods.add(
        		collectStats(submissions, getTime, getPeriodsOfMonths(24)));
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

    /**
     * If today is 12-12-2021 the functions returns periods starting at 01-12-2021,
     * 01-11-2021, etc up to "last" months ago.
     */
    private List<PeriodDelta> getPeriodsOfMonths(int last) {
        List<PeriodDelta> periods = new ArrayList<>();
        ZonedDateTime beginingOfMonth = ZonedDateTime.now()
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault());

        for (int i = 0; i <= last; i++) {
            ZonedDateTime beginning = beginingOfMonth.minus(Period.ofMonths(i));
            String name = beginning.getMonth().name().substring(0, 3);
            periods.add(new PeriodDelta(name, beginning));
        }
        return periods;
    }

    /**
     * Returns a list of periods up to "last" day ago.
     * The first element is the beginning of the current day.
     */
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

    private static <T> List<PeriodDelta> collectStats(List<T> entities,
    		Function<T, Instant> getTime,
    		List<PeriodDelta> periods) {
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
        return periods;
    }

    /**
     * @param getStatsForPeriod function (date from, date to) -> num elements
     * @param periods
     */
    private static List<PeriodDelta> collectStats(
    		BiFunction<ZonedDateTime, ZonedDateTime, Integer> getStatsForPeriod,
    		List<PeriodDelta> periods) {
    	ZonedDateTime to = ZonedDateTime.now();
    	for (PeriodDelta period : periods) {
    		ZonedDateTime from = period.timestamp;
    		period.delta = getStatsForPeriod.apply(from, to);
    		to = from;
    	}
    	return periods;
    }
}
