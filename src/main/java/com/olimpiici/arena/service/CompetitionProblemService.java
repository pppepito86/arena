package com.olimpiici.arena.service;

import com.olimpiici.arena.service.dto.CompetitionProblemDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service Interface for managing CompetitionProblem.
 */
public interface CompetitionProblemService {

    /**
     * Save a competitionProblem.
     *
     * @param competitionProblemDTO the entity to save
     * @return the persisted entity
     */
    CompetitionProblemDTO save(CompetitionProblemDTO competitionProblemDTO);

    /**
     * Get all the competitionProblems.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    Page<CompetitionProblemDTO> findAll(Pageable pageable);


    /**
     * Get the "id" competitionProblem.
     *
     * @param id the id of the entity
     * @return the entity
     */
    Optional<CompetitionProblemDTO> findOne(Long id);

    /**
     * Delete the "id" competitionProblem.
     *
     * @param id the id of the entity
     */
    void delete(Long id);
}
