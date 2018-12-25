package com.olimpiici.arena.service;

import com.olimpiici.arena.service.dto.SubmissionDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service Interface for managing Submission.
 */
public interface SubmissionService {

    /**
     * Save a submission.
     *
     * @param submissionDTO the entity to save
     * @return the persisted entity
     */
    SubmissionDTO save(SubmissionDTO submissionDTO);

    /**
     * Get all the submissions.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    Page<SubmissionDTO> findAll(Pageable pageable);


    /**
     * Get the "id" submission.
     *
     * @param id the id of the entity
     * @return the entity
     */
    Optional<SubmissionDTO> findOne(Long id);

    /**
     * Delete the "id" submission.
     *
     * @param id the id of the entity
     */
    void delete(Long id);
    
    Page<SubmissionDTO> findSubmissionsByCompetitionProblemId(Long competitionProblemId, Pageable pageable);
    
    Page<SubmissionDTO> findSubmissionsByCompetitionId(Long competitionId, Pageable pageable);
    
    String findSubmissionCode(Long id);
}
