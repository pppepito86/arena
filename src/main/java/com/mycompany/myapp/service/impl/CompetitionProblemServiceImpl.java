package com.mycompany.myapp.service.impl;

import com.mycompany.myapp.service.CompetitionProblemService;
import com.mycompany.myapp.domain.CompetitionProblem;
import com.mycompany.myapp.repository.CompetitionProblemRepository;
import com.mycompany.myapp.service.dto.CompetitionProblemDTO;
import com.mycompany.myapp.service.mapper.CompetitionProblemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing CompetitionProblem.
 */
@Service
@Transactional
public class CompetitionProblemServiceImpl implements CompetitionProblemService {

    private final Logger log = LoggerFactory.getLogger(CompetitionProblemServiceImpl.class);

    private final CompetitionProblemRepository competitionProblemRepository;

    private final CompetitionProblemMapper competitionProblemMapper;

    public CompetitionProblemServiceImpl(CompetitionProblemRepository competitionProblemRepository, CompetitionProblemMapper competitionProblemMapper) {
        this.competitionProblemRepository = competitionProblemRepository;
        this.competitionProblemMapper = competitionProblemMapper;
    }

    /**
     * Save a competitionProblem.
     *
     * @param competitionProblemDTO the entity to save
     * @return the persisted entity
     */
    @Override
    public CompetitionProblemDTO save(CompetitionProblemDTO competitionProblemDTO) {
        log.debug("Request to save CompetitionProblem : {}", competitionProblemDTO);

        CompetitionProblem competitionProblem = competitionProblemMapper.toEntity(competitionProblemDTO);
        competitionProblem = competitionProblemRepository.save(competitionProblem);
        return competitionProblemMapper.toDto(competitionProblem);
    }

    /**
     * Get all the competitionProblems.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CompetitionProblemDTO> findAll(Pageable pageable) {
        log.debug("Request to get all CompetitionProblems");
        return competitionProblemRepository.findAll(pageable)
            .map(competitionProblemMapper::toDto);
    }


    /**
     * Get one competitionProblem by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<CompetitionProblemDTO> findOne(Long id) {
        log.debug("Request to get CompetitionProblem : {}", id);
        return competitionProblemRepository.findById(id)
            .map(competitionProblemMapper::toDto);
    }

    /**
     * Delete the competitionProblem by id.
     *
     * @param id the id of the entity
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete CompetitionProblem : {}", id);
        competitionProblemRepository.deleteById(id);
    }
}
