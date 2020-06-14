package com.olimpiici.arena.service;

import com.olimpiici.arena.domain.UserPoints;
import com.olimpiici.arena.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class UserPointsService {

    private final Logger log = LoggerFactory.getLogger(UserPointsService.class);

    private final CompetitionRepository competitionRepository;

    private final SubmissionRepository submissionRepository;

    private final UserRepository userRepository;

    private final CompetitionService competitionService;

    public UserPointsService(
        CompetitionRepository competitionRepository,
        SubmissionRepository submissionRepository,
        UserRepository userRepository,
        CompetitionService competitionService) {
        this.competitionRepository = competitionRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.competitionService = competitionService;
    }

    /**
     * Returns a specific page of standings. Helps with caching.
     *
     * @param competitionId the competition
     * @param pageable      pagination information
     * @return a page of UserPoints
     */
    public Page<UserPoints> findStandingsPage(Long competitionId, Pageable pageable) {

        List<UserPoints> standings = competitionService.findStandings(competitionId);

        int fromIndex = (int) (pageable.getOffset());
        int toIndex = Math.min(standings.size(), (int) (pageable.getOffset() + pageable.getPageSize()));
        List<UserPoints> pageContent;

        if (fromIndex < standings.size()) {
            pageContent = standings.subList(fromIndex, toIndex);
        } else {
            pageContent = new ArrayList<>();
        }

        return new PageImpl<>(pageContent, pageable, standings.size());
    }


}
