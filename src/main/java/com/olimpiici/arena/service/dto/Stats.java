package com.olimpiici.arena.service.dto;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Stats {
    public Metric users = new Metric();
    public Metric activeUsers = new Metric();
    public Metric submissions = new Metric();
    public Problems problems = new Problems();
    public Queue queue = new Queue();
    public List<Long> badSubmissionIds;

    public static class Metric {
        public long total = -1;
        public List<List<PeriodDelta>> periods = new ArrayList<>();
    }

    // The change of a value during a period of time
    public static class PeriodDelta {
        public PeriodDelta(String name, ZonedDateTime timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }

        public String name;
        public ZonedDateTime timestamp;
        public long delta;
    }

    public static class Problems {
        public long total;
        public long submitable;
    }

    public static class QueuedSubmission {
        public Long id;
        public String author;
        public String problemName;
        public String verdict;
    }

    public static class Queue {
        public List<Long> submissionIds;
        public List<QueuedSubmission> submissions;

        // The age of the oldest submissions in queue
        public int ageMins;
    }
}
