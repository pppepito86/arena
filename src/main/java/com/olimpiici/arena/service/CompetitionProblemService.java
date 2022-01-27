package com.olimpiici.arena.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.grader.WorkerPool;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.mapper.CompetitionProblemMapper;

/**
 * Service for managing CompetitionProblem.
 */
@Service
@Transactional
public class CompetitionProblemService {

    private final Logger log = LoggerFactory.getLogger(CompetitionProblemService.class);

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private WorkerPool workerPool;

    private final CompetitionProblemRepository competitionProblemRepository;
    private final CompetitionProblemMapper competitionProblemMapper;
    private final SubmissionService submissionService;
    private final ProblemService problemService;


    public CompetitionProblemService(CompetitionProblemRepository competitionProblemRepository,
    			CompetitionProblemMapper competitionProblemMapper,
    			SubmissionService submissionService,
        		ProblemService problemService) {
        this.competitionProblemRepository = competitionProblemRepository;
        this.competitionProblemMapper = competitionProblemMapper;
        this.submissionService = submissionService;
        this.problemService = problemService;
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

	public void submitAuthorsIfNeeded(Long id) throws IOException {
    	CompetitionProblemDTO competitionProblemDTO = findOne(id).get();


    	Long problemId = competitionProblemDTO.getProblemId();
		Optional<File> maybeAuthor = problemService
    			.getAuthorSolution(applicationProperties.getWorkDir(), problemId);
    	if (!maybeAuthor.isPresent()) {
    		log.error("No author solution for problem " + competitionProblemDTO.getTitle() + "(" + problemId + ")");
    		return;
    	}
    	File author = maybeAuthor.get();

    	SubmissionDTO submission = new SubmissionDTO();
    	submission.setCompetitionProblemId(id);
    	submission.setUserId(4L);

        String extension = FilenameUtils.getExtension(author.getName()).toLowerCase();
    	for (int i = 0; i < 3; i++) {
    		SubmissionDTO s = submissionService.save(submission);
    		File submissionFile = Paths.get(applicationProperties.getWorkDir(), "submissions", ""+s.getId(), "solution." + extension).toFile();
    		FileUtils.copyFile(author, submissionFile);
    		s.setVerdict("waiting");
    		submissionService.save(s);
    	}

	}

	public void autoSetTimeLimits() throws Exception {
	       PageRequest page = PageRequest.of(0, 10000);
	       List<ProblemDTO> problems = problemService.findAll(page).getContent();
	       log.debug("Setting time limit for " + problems.size() + " problems");
	       for (ProblemDTO problem : problems) {
	           autoSetTimeLimits(problem.getId());
	       }
	    }

	public void autoSetTimeLimits(Long compProblemId) throws Exception {
        // TODO: use problem id, not comp problem id
        log.debug("Setting automatic time limit for problem " + compProblemId);
        PageRequest page = PageRequest.of(0, 10000);
        List<SubmissionDTO> submissions = submissionService
            .findSubmissionsByCompetitionProblem(compProblemId, page)
            .getContent();

        long problemId = findOne(compProblemId).get().getProblemId();

        final int authorUserId = 4;
        final int numSolutions = 3;

        boolean hasEnoughGoodSubmitions = submissions
                            .stream()
                            .filter(s -> s.getUserId() == authorUserId && s.getPoints() == 100)
                            .count() >= numSolutions;

        if (hasEnoughGoodSubmitions) {
            List<Integer> times = submissions.stream()
                    .filter(s -> s.getUserId() == authorUserId && s.getPoints() != null && s.getPoints() == 100)
                    .limit(numSolutions)
                    .map(s -> s.getTimeInMillis())
                    .collect(Collectors.toList());

            int max = times.stream().mapToInt(t -> t).max().getAsInt();

            int timeLimitMs = 100 * ((max*15/10)/100+1);

            log.info("limit for problem<" + problemId + "> with times " +
                    times + " will be " + timeLimitMs + "ms");

            problemService.updateTimeLimit(problemId, timeLimitMs);
            workerPool.deleteProblem(problemId);
        } else {
            log.info("can't find " + numSolutions + " good solutions for problemId = " + problemId);
        }
	}
}
