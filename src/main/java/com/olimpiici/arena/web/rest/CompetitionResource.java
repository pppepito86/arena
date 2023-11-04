package com.olimpiici.arena.web.rest;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.domain.UserPoints;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.security.SecurityUtils;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.CompetitionService;
import com.olimpiici.arena.service.ProblemService;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.service.dto.CompetitionDTO;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.util.RandomUtil;
import com.olimpiici.arena.web.rest.errors.BadRequestAlertException;
import com.olimpiici.arena.web.rest.util.HeaderUtil;
import com.olimpiici.arena.web.rest.util.PaginationUtil;

import io.github.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing Competition.
 */
@RestController
@RequestMapping("/api")
public class CompetitionResource {

    private final Logger log = LoggerFactory.getLogger(CompetitionResource.class);
    private static final String ENTITY_NAME = "competition";
    private final CompetitionService competitionService;
    @Autowired
    private CompetitionProblemService competitionProblemService;
    @Autowired
    private SubmissionRepository submissionRepository;
    private final SubmissionService submissionService;
    private final UserRepository userRepository;
    private final ProblemService problemService;
    @Autowired
    private ApplicationProperties applicationProperties;

    private final static int AUTHOR_ID = 4;
    private final static int PESHO_ORGOV_ID = 2032;
    private final static Duration DURATION_BETWEEN_SUBMITS = Duration.ofSeconds(20);
    private final static int MAX_SUBMISSIONS_PER_DAY = 150;

    public CompetitionResource(CompetitionService competitionService,
    		UserRepository userRepository,
    		SubmissionService submissionService,
    		ProblemService problemService) {
        this.competitionService = competitionService;
        this.userRepository = userRepository;
        this.submissionService = submissionService;
        this.problemService = problemService;
    }

    /**
     * POST  /competitions : Create a new competition.
     *
     * @param competitionDTO the competitionDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new competitionDTO, or with status 400 (Bad Request) if the competition has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/competitions")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<CompetitionDTO> createCompetition(
    		@RequestBody CompetitionDTO competitionDTO) throws URISyntaxException {
        log.debug("REST request to save Competition : {}", competitionDTO);
        if (competitionDTO.getId() != null) {
            throw new BadRequestAlertException(
            		"A new competition cannot already have an ID", ENTITY_NAME, "idexists");
        }
        CompetitionDTO result = competitionService.save(competitionDTO);
        return ResponseEntity.created(new URI("/api/competitions/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PostMapping("/competitions/{id}/subcompetitions")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<CompetitionDTO> updateSubCompetitions(@PathVariable Long id,
    		@RequestBody List<CompetitionDTO> subCompetitions) throws URISyntaxException {
        log.debug("REST request to set cubcompetitions: {}", id);

        competitionService.updateSubCompetitions(id, subCompetitions);;
        return ResponseEntity.ok().build();
    }

    @PostMapping("/competitions/{id}/subproblems")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<CompetitionDTO> updateSubProblems(@PathVariable Long id,
    		@RequestBody List<CompetitionProblemDTO> subProblems) throws URISyntaxException {
        log.debug("REST request to set subproblems: {}", id);

        competitionService.updateSubProblems(id, subProblems);
        return ResponseEntity.ok().build();
    }

    /**
     * PUT  /competitions : Updates an existing competition.
     *
     * @param competitionDTO the competitionDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated competitionDTO,
     * or with status 400 (Bad Request) if the competitionDTO is not valid,
     * or with status 500 (Internal Server Error) if the competitionDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/competitions")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<CompetitionDTO> updateCompetition(
    		@RequestBody CompetitionDTO competitionDTO) throws URISyntaxException {
        log.debug("REST request to update Competition : {}", competitionDTO);
        if (competitionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        CompetitionDTO result = competitionService.save(competitionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, competitionDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /competitions : get all the competitions.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of competitions in body
     */
    @GetMapping("/competitions")
    @Timed
    public ResponseEntity<List<CompetitionDTO>> getAllCompetitions(Pageable pageable) {
        log.debug("REST request to get a page of Competitions");
        Page<CompetitionDTO> page = competitionService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/competitions");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET  /competitions/:id : get the "id" competition.
     *
     * @param id the id of the competitionDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the competitionDTO, or with status 404 (Not Found)
     */
    @GetMapping("/competitions/{id}")
    @Timed
    public ResponseEntity<CompetitionDTO> getCompetition(@PathVariable Long id) {
        log.debug("REST request to get Competition : {}", id);
        Optional<CompetitionDTO> competitionDTO = competitionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(competitionDTO);
    }

    /**
     * GET  /competitions/:id/children : get the children of "id" competition.
     *
     * @param id of the competitionDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body list of competitionDTO, or with status 404 (Not Found)
     */
    @GetMapping("/competitions/{id}/children")
    @Timed
    public ResponseEntity<List<CompetitionDTO>> getCompetitionChildren(
    		@PathVariable Long id, Pageable pageable) {
        log.debug("REST request to get Competition children : {}", id);
        Page<CompetitionDTO> page = competitionService.findChildren(id, pageable);
        String url = String.format("/api/competitions/%d/children", id);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, url);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/competitions/{id}/path")
    @Timed
    public ResponseEntity<List<CompetitionDTO>> getCompetitionPath(@PathVariable Long id) {
        log.debug("REST request to get Competition path : {}", id);
        List<CompetitionDTO> path = competitionService.findPathFromRoot(id);
        return ResponseEntity.ok(path);
    }

    @GetMapping("/competitions/{id}/submissions")
    @Timed
    public ResponseEntity<List<SubmissionDTO>> getCompetitionSubmissions(
    		@PathVariable Long id, Pageable pageable) {
        log.debug("REST request to get Competition submissoins : {}", id);
        Page<SubmissionDTO> page;

        if (SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN)) {
        	page = competitionService.findSubmissionsByCompetition(id, pageable);
        } else {
        	User user =
        			userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin().get()).get();
        	page = competitionService.findSubmissionsByCompetitionAndUser(user, id, pageable);
        }
        String url = String.format("/api/competitions/{id}/problem/{compProb}/submissions", id);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, url);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/competitions/{id}/problems")
    @Timed
    public ResponseEntity<List<CompetitionProblemDTO>> getCompetitionProblems(
    		@PathVariable Long id, Pageable pageable, Principal principal) {
        log.debug("REST request to get Competition problems : {}", id);

        User user = userRepository
        		.findOneByLogin(SecurityUtils.getCurrentUserLogin().get())
        		.get();

        Page<CompetitionProblemDTO> page = competitionService.findProblems(id, pageable);
        for (CompetitionProblemDTO dto : page.getContent()) {

        	Integer points = competitionService.findPointsForCompetitionProblem(user, dto.getId());
        	dto.setPoints(points);
        }
        String url = String.format("/api/competitions/%d/problems", id);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, url);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/competitions/{id}/problem/{compProb}")
    @Timed
    public ResponseEntity<ProblemDTO> getCompetitionProblem(
    		@PathVariable Long id, @PathVariable Long compProb) {
        log.debug("REST request to get Competition problems : {}", id);
        ProblemDTO problem = competitionService.findProblem(compProb);
        problemService.setLimitsToDto(problem);
        return ResponseEntity.ok(problem);
    }

    private void maybeThrottleSubmission(Long userId) {
        if (SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN)) {
            return;
        }
        
        if (userId == AUTHOR_ID || userId == PESHO_ORGOV_ID) {
            return;
        }

        Optional<java.sql.Timestamp > lastSubmission = submissionRepository.findLastByUser(userId);
        if (!lastSubmission.isPresent())  {
            return;
        }
        Duration d = Duration.between(lastSubmission.get().toInstant(), ZonedDateTime.now());
        log.error("Duration " + d.toMillis()/1000);
        if (d.compareTo(DURATION_BETWEEN_SUBMITS) < 0) {
            throw new BadRequestAlertException("Submission throttled.", "submission", "submission-throttled");
        }

        if (submissionRepository.numSubmissionsLastDay(userId) > MAX_SUBMISSIONS_PER_DAY) {
            throw new BadRequestAlertException("Submission throttled.", "submission", "too-many-submissions-per-day");
        }
    }

    @PostMapping("/competitions/{id}/problem/{compProb}/submit")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.USER + "\")")
    @Timed
    public ResponseEntity<SubmissionDTO> submitProblem(@PathVariable Long id,
    		@PathVariable Long compProb,
    		@RequestBody String solution) throws Exception {
        log.debug("REST request to submit solution : {}", solution);
        User user = userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin().get()).get();
        
        maybeThrottleSubmission(user.getId());

        SubmissionDTO submission = new SubmissionDTO();
        submission.setUserId(user.getId());
        submission.setCompetitionProblemId(compProb);
        submission.setUploadDate(ZonedDateTime.now());
        submission.setSecurityKey(RandomUtil.generateSubmissionSecurityKey());
        submission = submissionService.save(submission);

        // max allowed source code size is 64 KB UTF-8
        if (solution.length() > 64*1024/4) {
            submission.setVerdict("source too large");
        	submission.setDetails("source too large");
        } else {
            long problemId = competitionProblemService.findOne(compProb).get().getProblemId();
            String ext = problemService.getSolutionFileExtension(problemId);

            File submissionsDir = new File(applicationProperties.getWorkDir(), "submissions");
	        File submissionDir = new File(submissionsDir, ""+submission.getId());
	        File submissionFile = new File(submissionDir, "solution." + ext);

	        submissionDir.mkdirs();
	        FileUtils.writeStringToFile(submissionFile, solution, StandardCharsets.UTF_8);
	        submission.setVerdict("waiting");
        }

        submissionService.save(submission);

        return ResponseEntity.ok(submission);
    }

    @GetMapping("/competitions/{id}/problem/{compProb}/submissions")
    @Timed
    public ResponseEntity<List<SubmissionDTO>> getSubmissions(@PathVariable Long id,
    		@PathVariable Long compProb, Pageable pageable) {
        log.debug("REST request to get submission for competitive problem : {}", compProb);
        Page<SubmissionDTO> page;
        if (SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN)) {
        	page = submissionService.findSubmissionsByCompetitionProblem(compProb, pageable);
        } else {
        	User user =
        			userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin().get()).get();
        	page = submissionService.findSubmissionsByCompetitionProblemAndUser(user, compProb, pageable);
        }
        String url = String.format("/api/competitions/{id}/problem/{compProb}/submissions", id);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, url);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/competitions/{id}/standings")
    @Timed
    public ResponseEntity<List<UserPoints>> getStandings(@PathVariable Long id, Pageable pageable,
            @RequestParam(value = "w", defaultValue = "5200") Integer weeks,
            @RequestParam(value = "f", required = false) List<String> filter) {
        log.debug("REST request to get standings for competition: {}, weeks = {}, filter = {}", id, weeks, filter);
        ZonedDateTime from = ZonedDateTime.now().minus(Period.ofWeeks(weeks));

        if (filter != null && filter.isEmpty()) filter = null;
        Page<UserPoints> page = competitionService.findStandings(id, pageable, from, filter);

        String url = String.format("/api/competitions/{id}/standings", id);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, url);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/competitions/{id}/mypoints")
    @Timed
    public UserPoints getStandingsForCurrentUser(@PathVariable Long id,
            @RequestParam(value = "w", defaultValue = "5200") Integer weeks,
            @RequestParam(value = "f", required = false) List<String> filter) {

    	User user = userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin().get()).get();
    	ZonedDateTime from = ZonedDateTime.now().minus(Period.ofWeeks(weeks));


        if (filter != null && filter.isEmpty()) filter = null;
        return competitionService.findPointsForUser(id, user.getId(), from, filter);
    }

    /**
     * DELETE  /competitions/:id : delete the "id" competition.
     *
     * @param id the id of the competitionDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/competitions/{id}")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteCompetition(@PathVariable Long id) {
        log.debug("REST request to delete Competition : {}", id);
        competitionService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
