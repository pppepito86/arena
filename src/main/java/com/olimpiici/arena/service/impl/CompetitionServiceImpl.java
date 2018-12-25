package com.olimpiici.arena.service.impl;

import com.olimpiici.arena.service.CompetitionService;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.domain.UserPoints;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.CompetitionRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.dto.CompetitionDTO;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.mapper.CompetitionMapper;
import com.olimpiici.arena.service.mapper.CompetitionProblemMapper;
import com.olimpiici.arena.service.mapper.ProblemMapper;
import com.olimpiici.arena.service.mapper.SubmissionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service Implementation for managing Competition.
 */
@Service
@Transactional
public class CompetitionServiceImpl implements CompetitionService {

    private final Logger log = LoggerFactory.getLogger(CompetitionServiceImpl.class);

    private final CompetitionRepository competitionRepository;
    
    private final CompetitionProblemRepository competitionProblemRepository;
    
    private final SubmissionRepository submissionRepository;

    private final CompetitionMapper competitionMapper;
    
    private final ProblemMapper problemMapper;
    
    private final CompetitionProblemMapper competitionProblemMapper;
    
    private final SubmissionMapper submissionMapper;
    
    private final SubmissionService submissionService;
    
    private final UserRepository userRepository;

    public CompetitionServiceImpl(CompetitionRepository competitionRepository, 
    		CompetitionProblemRepository competitionProblemRepository, 
    		CompetitionMapper competitionMapper,
    		ProblemMapper problemMapper,
    		CompetitionProblemMapper competitionProblemMapper,
    		SubmissionRepository submissionRepository,
    		SubmissionMapper submissionMapper,
    		SubmissionService submissionService,
    		UserRepository userRepository) {
        this.competitionRepository = competitionRepository;
        this.competitionMapper = competitionMapper;
        this.problemMapper = problemMapper;
        this.competitionProblemRepository = competitionProblemRepository;
        this.competitionProblemMapper = competitionProblemMapper;
        this.submissionRepository = submissionRepository;
        this.submissionMapper = submissionMapper;
        this.submissionService = submissionService;
        this.userRepository = userRepository;
    }

    /**
     * Save a competition.
     *
     * @param competitionDTO the entity to save
     * @return the persisted entity
     */
    @Override
    public CompetitionDTO save(CompetitionDTO competitionDTO) {
        log.debug("Request to save Competition : {}", competitionDTO);

        Competition competition = competitionMapper.toEntity(competitionDTO);
        competition = competitionRepository.save(competition);
        return competitionMapper.toDto(competition);
    }

    /**
     * Get all the competitions.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CompetitionDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Competitions");
        return competitionRepository.findAll(pageable)
            .map(competitionMapper::toDto);
    }


    /**
     * Get one competition by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<CompetitionDTO> findOne(Long id) {
        log.debug("Request to get Competition : {}", id);
        return competitionRepository.findById(id)
            .map(competitionMapper::toDto);
    }

    /**
     * Delete the competition by id.
     *
     * @param id the id of the entity
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete Competition : {}", id);
        competitionRepository.deleteById(id);
    }

	@Override
	public Page<CompetitionDTO> findChildren(Long id, Pageable pageable) {
		log.debug("Request to get all children for Competition {}", id);
		Optional<Competition> parent = competitionRepository.findById(id);
		if (parent.isPresent()) {
	        return competitionRepository.findByParent(parent.get(), pageable)
	            .map(competitionMapper::toDto);
		} else {
			return Page.empty(pageable);
		}
	}

	@Override
	public List<CompetitionDTO> findPathFromRoot(Long id) {
		Optional<CompetitionDTO> res = competitionRepository.findById(id)
				.map(competitionMapper::toDto);
		if (!res.isPresent()) return null;
		
		CompetitionDTO curr = res.get();
		CompetitionDTO parent;
		List<CompetitionDTO> path = new ArrayList<CompetitionDTO>();
		path.add(curr);
		
		while (curr.getId() != curr.getParentId() && curr.getParentId() != null) {
			Optional<CompetitionDTO> parentRes = competitionRepository
					.findById(curr.getParentId())
					.map(competitionMapper::toDto);
			if (!parentRes.isPresent()) break;
			parent = parentRes.get();
			path.add(parent);
			curr = parent; 
		}
		
		Collections.reverse(path);
		return path;
	}

	@Override
	public Page<CompetitionProblemDTO> findProblems(Long id, Pageable pageable) {
		log.debug("Request to get all problems for Competition {}", id);
		Competition competition = competitionRepository.findById(id).get();
		
		Page<CompetitionProblemDTO> problems = 
				competitionProblemRepository
				.findByCompetition(competition , pageable)
				.map(problem -> {
					CompetitionProblemDTO dto = competitionProblemMapper.toDto(problem);
					dto.setTitle(problem.getProblem().getTitle());
					return dto;
				});
		
		return problems;
	}

	@Override
	public ProblemDTO findProblem(Long competitionProblem) {
		Problem problem = competitionProblemRepository
			.findById(competitionProblem)
			.get()
			.getProblem();
		
		ProblemDTO dto = problemMapper.toDto(problem);
		return dto;
	}
	
	@Override
	public Page<SubmissionDTO> findSubmissions(Long competitionProblemId, Pageable pageable) {
		return submissionService.findSubmissionsByCompetitionProblemId(competitionProblemId, pageable);
	}

	@Override
	public Integer findPointsForCompetitionProblem(User user, Long competitionProblemId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer findPointsForCompetition(User user, Long competitionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer findTotalPoints(User user) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<UserPoints> findStandings(Long competitionId, Pageable pageable) {
		// TODO Auto-generated method stub
		
		Competition competition = competitionRepository.getOne(competitionId);
		Map<User, Integer> standings = new HashMap<>();
		standings.put(userRepository.getOne(1L), 1231);
		standings.put(userRepository.getOne(2L), 325);
		standings.put(userRepository.getOne(3L), 634);
		
		
		List<UserPoints> list = new ArrayList<UserPoints>();
		for(Map.Entry<User, Integer> entry : standings.entrySet()) {
			list.add(new UserPoints(entry.getKey(), entry.getValue()));
		}
		Collections.sort(list);
		
		int fromIndex = (int)(pageable.getOffset());
		int toIndex = Math.min(list.size(), (int)(pageable.getOffset() + pageable.getPageSize()));
		List<UserPoints> pageContent;
		
		if (fromIndex < list.size()) {
			pageContent = list.subList(fromIndex, toIndex);
		} else {
			pageContent = new ArrayList<>();
		}
		
		return new PageImpl<>(pageContent, pageable, list.size());
	}
}
