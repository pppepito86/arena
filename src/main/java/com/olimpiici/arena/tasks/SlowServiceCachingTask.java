package com.olimpiici.arena.tasks;

import com.olimpiici.arena.service.CompetitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SlowServiceCachingTask {


    private final Logger log = LoggerFactory.getLogger(SlowServiceCachingTask.class);

    private final CompetitionService competitionService;

    private final CacheManager cacheManager;

    @Autowired
    public SlowServiceCachingTask(CompetitionService competitionService, CacheManager cacheManager) {
        this.competitionService = competitionService;
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedDelay = 3 * 60 * 1000)
    public void refreshGlobalStandingsTask() {

        log.debug("Refreshing global standings cache.");

        cacheManager.getCache(CompetitionService.USER_POINTS_CACHE).evict((long) 1);

        competitionService.findStandings((long) 1);
    }
}
