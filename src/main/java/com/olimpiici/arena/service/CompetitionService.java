package com.olimpiici.arena.service;

import java.math.BigDecimal;
import java.math.BigInteger;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

/**
 * Service ementation for managing Competition.
 */
@Service
@Transactional
public class CompetitionService {

    private final Logger log = LoggerFactory.getLogger(CompetitionService.class);

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private CompetitionProblemRepository competitionProblemRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private CompetitionMapper competitionMapper;

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private CompetitionProblemMapper competitionProblemMapper;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private UserRepository userRepository;

	@Autowired
    private ProblemRepository problemRepository;

	private final int MAX_PROBLEM_COLUMNS = 10;

    /**
     * Save a competition.
     *
     * @param competitionDTO the entity to save
     * @return the persisted entity
     */

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

    public void delete(Long id) {
        log.debug("Request to delete Competition : {}", id);
        competitionRepository.deleteById(id);
    }


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


	public ProblemDTO findProblem(Long competitionProblem) {
		Problem problem = competitionProblemRepository
			.findById(competitionProblem)
			.get()
			.getProblem();

		ProblemDTO dto = problemMapper.toDto(problem);
		return dto;
	}


	public Integer findPointsForCompetitionProblem(User user, Long competitionProblemId) {
		CompetitionProblem competitionProblem = competitionProblemRepository
				.findById(competitionProblemId).get();
		return findPointsForCompetitionProblem(user, competitionProblem);
	}


	public Integer findPointsForCompetitionProblem(User user, CompetitionProblem competitionProblem) {
		return submissionRepository
				.findByCompetitionProblemAndUser(competitionProblem, user)
				.stream()
				.map(submission -> submission.getPoints())
				.reduce(0, IntUtil::safeMax);
	}


	public Integer findPointsForCompetition(User user, Long competitionId) {
		Competition competition = competitionRepository.getOne(competitionId);
		return competitionProblemRepository
				.findByCompetition(competition)
				.stream()
				.map(cp -> findPointsForCompetitionProblem(user, cp))
				.reduce(0, IntUtil::safeSum);
	}


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

	/**
	 *
	 * @param competitionId
	 * @param pageable
	 * @param from Start date for the standings generation.
	 * @param filter If null, no filtering is applied. Otherwise only sumbissions to problems
	 * in competitions with name equal to one of the elements of this list are considered.
	 * For exaple if filter = {'C', 'D'} only submissions for C and D group will be considered.
	 * @return
	 */
	public Page<UserPoints> findStandings(Long competitionId, Pageable pageable,
            ZonedDateTime from, List<String> filter) {
		log.debug("Calculating standings for competition {} from {} with filter {}", competitionId,
			from, filter == null ? "null" : filter.toString());

		List<Object[]> rawStandings;
		int size;
		if (competitionId == 1 && filter == null) {
			// For the root competition with no filters compute standing directly without
			// sending SQL filters for the problems. This makes the computation around 3x faster
			// (600ms vs 200ms) and having a separate branch is worth it since this is the most
			// commonly requested standings.
			rawStandings = competitionRepository
					.getRootStandings(from, pageable.getOffset(), pageable.getPageSize());
			size = competitionRepository.getRootStandingsSize(from);
		} else {
			Competition competition = competitionRepository.getOne(competitionId);
			List<Long> problems = findAllProblemsInSubTree(competition, filter).stream()
					.map(cp -> cp.getId())
					.collect(Collectors.toList());

			if (problems.size() <= MAX_PROBLEM_COLUMNS) {
				rawStandings = competitionRepository
						.getStandingsPerProblemForProblems(from, problems,
								pageable.getOffset(), pageable.getPageSize());
			} else {
				rawStandings = competitionRepository
						.getAggregatedStandingsForProblems(from, problems,
								pageable.getOffset(), pageable.getPageSize());
			}

			size = competitionRepository.getAggregatedStandingsSizeForProblems(from, problems);
		}

		List<UserPoints> standings = rawStandings.stream()
				.map(this::parseRawStandingsRow)
				.collect(Collectors.toList());
		return new PageImpl<>(standings, pageable, size);
	}

	public UserPoints findPointsForUser(Long competitionId, Long userId, ZonedDateTime from, List<String> filter) {
		Competition competition = competitionRepository.getOne(competitionId);
		List<Long> problems = findAllProblemsInSubTree(competition, filter).stream()
				.map(cp -> cp.getId())
				.collect(Collectors.toList());
		return findPointsForUser(problems, userId, from, filter);
	}

	public Map<Long, Integer> findSimplePointsForUserPerProblem(List<Long> compProblemIds, Long userId) {
		Map<Long, Integer> pointsPerProblem = new HashMap<Long, Integer>();
		if (compProblemIds.isEmpty()) {
			// The SQL query below will crash if the set is empty,
			// so we prevent this by returning early.
			return pointsPerProblem;
		}

		List<Object[]> raw = competitionRepository.getSimpleUserPointsPerProblem(compProblemIds, userId);
		// 0 is the default if a problem is not returned but the SQL query
		for (Long problemId : compProblemIds) {
			pointsPerProblem.put(problemId, 0);
		}

		for (Object[] r : raw) {
			Long problemId = ((BigInteger)r[0]).longValue();
			Integer points = ((BigInteger)r[0]).intValue();
			pointsPerProblem.put(problemId, points);
		}
		return pointsPerProblem;
	}

	public UserPoints findPointsForUser(List<Long> compProblemIds, Long userId, ZonedDateTime from, List<String> filter) {
		List<Object[]> raw = compProblemIds.size() <= MAX_PROBLEM_COLUMNS
				? competitionRepository.getUserPointsPerProblem(from, compProblemIds, userId)
				: competitionRepository.getAggregatedUserPointsForProblems(from, compProblemIds, userId);

		if (raw.isEmpty()) {
			User user = userRepository.findById(userId).get();
			return new UserPoints(userId, user.getFirstName(), user.getLastName(), /*points=*/ 0);
		}

		return parseRawStandingsRow(raw.get(0));
	}

	private UserPoints parseRawStandingsRow(Object[] row) {
		Long userId = ((BigInteger)row[0]).longValue();
		String firstName = (String)row[1];
		String lastName = (String)row[2];
		Integer points = ((BigDecimal)row[3]).intValue();

		UserPoints userPoints = new UserPoints(userId, firstName, lastName, points);
		if (4 < row.length) {
			userPoints.perProblemJson = ((String)row[4]);
		}

		return userPoints;
	}

	public List<Competition> findAllCompetitionsInSubTree(Competition competition) {
		return findAllCompetitionsInSubTree(competition, null);
	}

	private boolean matchesFilter(String name, List<String> filter) {
		if (filter == null) {
			return true;
		}
		for (String f : filter) {
			if (name.equals(f)) {
				return true;
			}
		}
		return false;
	}

	public List<Competition> findAllCompetitionsInSubTree(Competition competition, List<String> filter) {
		List<Competition> bfs = new ArrayList<>();
		List<Competition> filteredCompetitions = new ArrayList<>();
		bfs.add(competition);
		// Find all competitions which match the filter using BFS.
		while (!bfs.isEmpty()) {
			List<Competition> next = competitionRepository.findByParentIn(bfs);
			bfs.clear();
			for (Competition c : next) {
				if (matchesFilter(c.getLabel(), filter)) {
					filteredCompetitions.add(c);
				} else {
					bfs.add(c);
				}
			}

			bfs = next;
		}

		// Find all competitions in the subtrees of filteredCompetitions
		List<Competition> result = new ArrayList<>();
		if (matchesFilter(competition.getLabel(), filter)) {
			result.add(competition);
		}
		result.addAll(filteredCompetitions);
		bfs = filteredCompetitions;
		while (!bfs.isEmpty()) {
			List<Competition> next = competitionRepository.findByParentIn(bfs);
			result.addAll(next);
			bfs = next;
		}

		return result;
	}

	public List<CompetitionProblem> findAllProblemsInSubTree(Competition competition) {
		return findAllProblemsInSubTree(competition, null);
	}

	public List<CompetitionProblem> findAllProblemsInSubTree(Competition competition, List<String> filter) {
		List<Competition> competitions = findAllCompetitionsInSubTree(competition, filter);
		List<CompetitionProblem> problems = competitionProblemRepository
			.findByCompetitionIn(competitions);
		return problems;
	}


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



	public Page<SubmissionDTO> findSubmissionsByCompetitionAndUser(Long userId, Long competitionId,
			Pageable pageable) {
		User user = userRepository.findById(userId).get();
		return findSubmissionsByCompetitionAndUser(user, competitionId, pageable);
	}


	public Page<SubmissionDTO> findSubmissionsByCompetitionAndUser(User user, Long competitionId, Pageable pageable) {
		Competition competition = competitionRepository.getOne(competitionId);
		List<CompetitionProblem> problems =
				findAllProblemsInSubTree(competition);
		Page<SubmissionDTO> submissions = submissionRepository
			.findByUserAndCompetitionProblemIn(user, problems, pageable)
			.map(submissionMapper::toDto);
		return submissions;
	}


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
