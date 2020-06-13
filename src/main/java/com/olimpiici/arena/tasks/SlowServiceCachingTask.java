package com.olimpiici.arena.tasks;

import com.olimpiici.arena.service.CompetitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.awt.print.Pageable;

@Component
public class SlowServiceCachingTask {

    private final CompetitionService competitionService;

    @Autowired
    public SlowServiceCachingTask(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @Scheduled(fixedDelay = 3 * 60 * 1000)
    public void refreshStandingsPages() {
        PageRequest firstPage = PageRequest.of(0, 20, Sort.by(Sort.Order.asc("id")));

        competitionService.findStandings((long) 1, firstPage);
    }
}
