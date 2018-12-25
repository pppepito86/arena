package com.olimpiici.arena.service;

import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.domain.UserPoints;
import com.olimpiici.arena.service.dto.CompetitionDTO;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service Interface for managing Competition.
 */
public interface CompetitionService {

    /**
     * Save a competition.
     *
     * @param competitionDTO the entity to save
     * @return the persisted entity
     */
    CompetitionDTO save(CompetitionDTO competitionDTO);

    /**
     * Get all the competitions.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    Page<CompetitionDTO> findAll(Pageable pageable);


    /**
     * Get the "id" competition.
     *
     * @param id the id of the entity
     * @return the entity
     */
    Optional<CompetitionDTO> findOne(Long id);

    /**
     * Delete the "id" competition.
     *
     * @param id the id of the entity
     */
    void delete(Long id);
    
    Page<CompetitionDTO> findChildren(Long id, Pageable pageable);
    
    List<CompetitionDTO> findPathFromRoot(Long id);
    
    Page<CompetitionProblemDTO> findProblems(Long id, Pageable pageable);
    
    ProblemDTO findProblem(Long competitionProblem);
    
    Page<SubmissionDTO> findSubmissions(Long competitionProblemId, Pageable pageable);
    
    Integer findPointsForCompetitionProblem(User user, Long competitionProblemId);
    
    Integer findPointsForCompetition(User user, Long competitionId);
    
    Integer findTotalPoints(User user);
    
    Page<UserPoints> findStandings(Long competitionId, Pageable pageable);
}
