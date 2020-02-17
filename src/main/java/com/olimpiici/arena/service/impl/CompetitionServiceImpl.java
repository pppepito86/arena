package com.olimpiici.arena.service.impl;

import com.olimpiici.arena.service.CompetitionService;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.service.Standings;
import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.domain.UserPoints;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.CompetitionRepository;
import com.olimpiici.arena.repository.ProblemRepository;
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
import com.olimpiici.arena.service.util.IntUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    
    private final ProblemRepository problemRepository;

    public CompetitionServiceImpl(CompetitionRepository competitionRepository, 
    		CompetitionProblemRepository competitionProblemRepository, 
    		CompetitionMapper competitionMapper,
    		ProblemMapper problemMapper,
    		CompetitionProblemMapper competitionProblemMapper,
    		SubmissionRepository submissionRepository,
    		SubmissionMapper submissionMapper,
    		SubmissionService submissionService,
    		UserRepository userRepository,
    		ProblemRepository problemRepository) {
        this.competitionRepository = competitionRepository;
        this.competitionMapper = competitionMapper;
        this.problemMapper = problemMapper;
        this.competitionProblemRepository = competitionProblemRepository;
        this.competitionProblemMapper = competitionProblemMapper;
        this.submissionRepository = submissionRepository;
        this.submissionMapper = submissionMapper;
        this.submissionService = submissionService;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
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
				.map(submission -> submission.getPoints())
				.reduce(0, IntUtil::safeMax);
	}

	@Override
	public Integer findPointsForCompetition(User user, Long competitionId) {
		Competition competition = competitionRepository.getOne(competitionId);
		return competitionProblemRepository
				.findByCompetition(competition)
				.stream()
				.map(cp -> findPointsForCompetitionProblem(user, cp))
				.reduce(0, IntUtil::safeSum);
	}

	@Override
	public Integer findTotalPoints(User user) {
		return submissionRepository
				.findByUser(user)
				.stream()
				.collect(Collectors.groupingBy( 
						Submission::getCompetitionProblem,
						Collectors.mapping(Submission::getPoints, 
								Collectors.reducing(0, IntUtil::safeMax))))
				.values()
				.stream()
				.reduce(0, IntUtil::safeSum);
	}

    
	@Override
	public Page<UserPoints> findStandings(Long competitionId, Pageable pageable) {
        ZonedDateTime from = ZonedDateTime.now().minus(Period.ofYears(100));
        return findStandings(competitionId, pageable, from);
    }

	@Override
	public Page<UserPoints> findStandings(Long competitionId, Pageable pageable, 
            ZonedDateTime from) {	
        Standings oldStandings = new Standings(userRepository);
		Standings fullStandings = new Standings(userRepository);
        Competition competition = competitionRepository.getOne(competitionId);
		List<CompetitionProblem> problems = findAllProblemsInSubTree(competition);
		submissionRepository
			.findByCompetitionProblemIn(problems)
			.stream()
			.forEach(submission -> {
                ZonedDateTime uploadDate = submission.getUploadDate();
                if (uploadDate != null && uploadDate.isBefore(from)) {
                    oldStandings.processSubmission(submission);
                }
                fullStandings.processSubmission(submission);
			});

        fullStandings.minus(oldStandings);
		List<UserPoints> standingsList = fullStandings.getStandings();

		int fromIndex = (int)(pageable.getOffset());
		int toIndex = Math.min(standingsList.size(), 
                (int)(pageable.getOffset() + pageable.getPageSize()));
		List<UserPoints> pageContent;
		
		if (fromIndex < standingsList.size()) {
			pageContent = standingsList.subList(fromIndex, toIndex);
		} else {
			pageContent = new ArrayList<>();
		}
		
		return new PageImpl<>(pageContent, pageable, standingsList.size());
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
	
	@Override
	public Page<SubmissionDTO> findSubmissionsByCompetition(
			Long competitionId, Pageable pageable) {
		Competition competition = competitionRepository.getOne(competitionId);
		List<CompetitionProblem> problems = 
				findAllProblemsInSubTree(competition);
		Page<SubmissionDTO> submissions = submissionRepository
			.findByCompetitionProblemIn(problems, pageable)
			.map(submissionMapper::toDto);
		return submissions;
	}
	
	
	@Override
	public Page<SubmissionDTO> findSubmissionsByCompetitionAndUser(Long userId, Long competitionId, 
			Pageable pageable) {
		User user = userRepository.findById(userId).get();
		return findSubmissionsByCompetitionAndUser(user, competitionId, pageable);
	}
	
	@Override
	public Page<SubmissionDTO> findSubmissionsByCompetitionAndUser(User user, Long competitionId, Pageable pageable) {
		Competition competition = competitionRepository.getOne(competitionId);
		List<CompetitionProblem> problems = 
				findAllProblemsInSubTree(competition);
		Page<SubmissionDTO> submissions = submissionRepository
			.findByUserAndCompetitionProblemIn(user, problems, pageable)
			.map(submissionMapper::toDto);
		return submissions;
	}

	@Override
	public void updateSubCompetitions(Long parentId, List<CompetitionDTO> newSubCompetitions) {
		Competition parent = competitionRepository.getOne(parentId);
		for (int i = 0; i < newSubCompetitions.size(); i++) {
			newSubCompetitions.get(i).setOrder(i);
		}
		
		Set<Long> childrenIds = new HashSet<>();
		for (CompetitionDTO dto : newSubCompetitions) {
			childrenIds.add(dto.getId());
			Competition competition = competitionRepository.getOne(dto.getId());
			boolean change = false;
			
			
			if (competition.getParent() == null || competition.getParent().getId() != parent.getId()) {
				change = true;
				competition.setParent(parent);
			}
			
			if (competition.getOrder() != dto.getOrder()) {
				change = true;
				competition.setOrder(dto.getOrder());
			}
			
			if (change) {
				competitionRepository.save(competition);
			}
		}
		
		competitionRepository.findByParent(parent, Pageable.unpaged())
			.forEach(comp -> {
				if (!childrenIds.contains(comp.getId())) {
					comp.setParent(null);
					competitionRepository.save(comp);
				}
			});
	}

	@Override
	public void updateSubProblems(Long parentId, List<CompetitionProblemDTO> newSubProblems) {
		Competition parent = competitionRepository.getOne(parentId);
		for (int i = 0; i < newSubProblems.size(); i++) {
			newSubProblems.get(i).setOrder(i);
		}
		Set<Long> childrenIds = new HashSet<>();
		for (CompetitionProblemDTO dto : newSubProblems) {
			
			CompetitionProblem cp;
			boolean change = false;
			if (dto.getId() != null) {			
				cp = competitionProblemRepository.getOne(dto.getId());
			} else {
				change = true;
				cp = new CompetitionProblem();
				cp.setCompetition(parent);
				Problem problem = problemRepository.getOne(dto.getProblemId());
				cp.setProblem(problem);
			}

			if (cp.getCompetition().getId() != parent.getId()) {
				change = true;
				cp.setCompetition(parent);
			}

			if (cp.getOrder() != dto.getOrder()) {
				change = true;
				cp.setOrder(dto.getOrder());
			}
			
			if (change) {
				cp = competitionProblemRepository.save(cp);
			}
			childrenIds.add(cp.getId());
		}
		
		competitionProblemRepository
			.findByCompetition(parent, Pageable.unpaged())
			.forEach(cp -> {
				if (!childrenIds.contains(cp.getId())) {
					cp.setCompetition(null);
					competitionProblemRepository.save(cp);
				}
			});
	}
}
