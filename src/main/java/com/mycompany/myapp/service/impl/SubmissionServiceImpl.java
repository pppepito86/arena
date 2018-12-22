package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.service.SubmissionService;
import com.mycompany.myapp.domain.Submission;
import com.mycompany.myapp.repository.SubmissionRepository;
import com.mycompany.myapp.service.dto.SubmissionDTO;
import com.mycompany.myapp.service.mapper.SubmissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing Submission.
 */
@Service
@Transactional
public class SubmissionServiceImpl implements SubmissionService {

    private final Logger log = LoggerFactory.getLogger(SubmissionServiceImpl.class);

    private final SubmissionRepository submissionRepository;

    private final SubmissionMapper submissionMapper;

    public SubmissionServiceImpl(SubmissionRepository submissionRepository, SubmissionMapper submissionMapper) {
        this.submissionRepository = submissionRepository;
        this.submissionMapper = submissionMapper;
    }

    /**
     * Save a submission.
     *
     * @param submissionDTO the entity to save
     * @return the persisted entity
     */
    @Override
    public SubmissionDTO save(SubmissionDTO submissionDTO) {
        log.debug("Request to save Submission : {}", submissionDTO);

        Submission submission = submissionMapper.toEntity(submissionDTO);
        submission = submissionRepository.save(submission);
        return submissionMapper.toDto(submission);
    }

    /**
     * Get all the submissions.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SubmissionDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Submissions");
        return submissionRepository.findAll(pageable)
            .map(submissionMapper::toDto);
    }


    /**
     * Get one submission by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<SubmissionDTO> findOne(Long id) {
        log.debug("Request to get Submission : {}", id);
        return submissionRepository.findById(id)
            .map(submissionMapper::toDto);
    }

    /**
     * Delete the submission by id.
     *
     * @param id the id of the entity
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete Submission : {}", id);
        submissionRepository.deleteById(id);
    }
}
