package com.olimpiici.arena.service.impl;

import com.olimpiici.arena.service.CompetitionService;
import com.olimpiici.arena.service.SubmissionService;
import com.google.common.collect.Comparators;
import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
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
import com.olimpiici.arena.web.rest.UserResource;

import liquibase.diff.compare.CompareControl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
	public Integer findPointsForCompetitionProblem(User user, Long competitionProblemId) {
		CompetitionProblem competitionProblem = competitionProblemRepository
				.findById(competitionProblemId).get();
		return findPointsForCompetitionProblem(user, competitionProblem);
	}
	
	@Override
	public Integer findPointsForCompetitionProblem(User user, CompetitionProblem competitionProblem) {
		return submissionRepository
				.findByCompetitionProblemAndUser(competitionProblem, user)
				.stream()
				.mapToInt(submission -> submission.getPoints())
				.max()
				.orElse(0);
	}

	@Override
	public Integer findPointsForCompetition(User user, Long competitionId) {
		Competition competition = competitionRepository.getOne(competitionId);
		return competitionProblemRepository
				.findByCompetition(competition)
				.stream()
				.mapToInt(cp -> findPointsForCompetitionProblem(user, cp))
				.sum();
	}

	@Override
	public Integer findTotalPoints(User user) {
		return submissionRepository
				.findByUser(user)
				.stream()
				.collect(Collectors.groupingBy( 
						Submission::getCompetitionProblem,
						Collectors.mapping(Submission::getPoints, 
											Collectors.maxBy(Comparator.naturalOrder()))))
				.values()
				.stream()
				.mapToInt(sum -> sum.orElse(0))
				.sum();
	}

	@Override
	public Page<UserPoints> findStandings(Long competitionId, Pageable pageable) {	
		Map<Long, Map<Long, Integer>> userToPointsPerProblem 
			= new HashMap<Long, Map<Long, Integer>>();
		Map<Long, User> idToUser = new HashMap<>();
		userRepository
			.findAll()
			.stream()
			.forEach(user -> idToUser.put(user.getId(), user));
		
		Competition competition = competitionRepository.getOne(competitionId);
		List<CompetitionProblem> problems = findAllProblemsInSubTree(competition);
		
		submissionRepository
			.findByCompetitionProblemIn(problems)
			.stream()
			.forEach(submission -> {
				Long userId = submission.getUser().getId();
				if (!userToPointsPerProblem.containsKey(userId)) 
					userToPointsPerProblem.put(userId, new HashMap<Long, Integer>());
				Map<Long, Integer> pointsPerProblem = userToPointsPerProblem.get(userId);
				Long problem = submission.getCompetitionProblem().getId();
				Integer points = pointsPerProblem.getOrDefault(problem, 0);
				points = Math.max(points, submission.getPoints());
				pointsPerProblem.put(problem, points);
			});
		
		List<UserPoints> standings = userToPointsPerProblem
			.entrySet()
			.stream()
			.map(entry -> {
				Integer points = entry.getValue()
						.values()
						.stream()
						.mapToInt(Integer::intValue)
						.sum();
				User user = idToUser.get(entry.getKey());
				return new UserPoints(user, points);
			}).collect(Collectors.toList());
		

		Collections.sort(standings);
		
		int fromIndex = (int)(pageable.getOffset());
		int toIndex = Math.min(standings.size(), (int)(pageable.getOffset() + pageable.getPageSize()));
		List<UserPoints> pageContent;
		
		if (fromIndex < standings.size()) {
			pageContent = standings.subList(fromIndex, toIndex);
		} else {
			pageContent = new ArrayList<>();
		}
		
		return new PageImpl<>(pageContent, pageable, standings.size());
	}

	@Override
	public List<Competition> findAllCompetitionsInSubTree(Competition competition) {
		List<Competition> all = new ArrayList<>();
		List<Competition> bfs = new ArrayList<>();
		
		all.add(competition);
		bfs.add(competition);
		while (!bfs.isEmpty()) {
			List<Competition> next = competitionRepository.findByParentIn(bfs);
			all.addAll(next);
			bfs = next;
		}
		
		return all;
	}

	@Override
	public List<CompetitionProblem> findAllProblemsInSubTree(Competition competition) {
		List<Competition> competitions = findAllCompetitionsInSubTree(competition);
		List<CompetitionProblem> problems = competitionProblemRepository
			.findByCompetitionIn(competitions);
		return problems;
	}

}
