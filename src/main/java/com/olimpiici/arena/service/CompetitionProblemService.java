package com.olimpiici.arena.service;

import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.mapper.CompetitionProblemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service ementation for managing CompetitionProblem.
 */
@Service
@Transactional
public class CompetitionProblemService {

    private final Logger log = LoggerFactory.getLogger(CompetitionProblemService.class);

    private final CompetitionProblemRepository competitionProblemRepository;

    private final CompetitionProblemMapper competitionProblemMapper;

    public CompetitionProblemService(CompetitionProblemRepository competitionProblemRepository, CompetitionProblemMapper competitionProblemMapper) {
        this.competitionProblemRepository = competitionProblemRepository;
        this.competitionProblemMapper = competitionProblemMapper;
    }

    /**
     * Save a competitionProblem.
     *
     * @param competitionProblemDTO the entity to save
     * @return the persisted entity
     */
    
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
    
    @Transactional(readOnly = true)
    public Optional<CompetitionProblemDTO> findOne(Long id) {
        log.debug("Request to get CompetitionProblem : {}", id);
        Optional<CompetitionProblem> compProblem = competitionProblemRepository.findById(id);
        if (!compProblem.isPresent()) return Optional.empty();
        
        Problem problem = compProblem.get().getProblem();
        CompetitionProblemDTO dto = competitionProblemMapper.toDto(compProblem.get());
        dto.setTitle(problem.getTitle());
        return Optional.of(dto);
    }

    /**
     * Delete the competitionProblem by id.
     *
     * @param id the id of the entity
     */
    
    public void delete(Long id) {
        log.debug("Request to delete CompetitionProblem : {}", id);
        competitionProblemRepository.deleteById(id);
    }

	
	public Optional<CompetitionProblemDTO> findOneByProblem(Long id) {
		Optional<CompetitionProblem> cp = competitionProblemRepository.findOneByProblemId(id);
		if(cp.isPresent()) {
			return Optional.of(competitionProblemMapper.toDto(cp.get()));
		} else {
			return Optional.empty();
		}
	}
}
