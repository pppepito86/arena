package com.olimpiici.arena.web.rest;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.domain.UserPoints;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.security.SecurityUtils;
import com.olimpiici.arena.service.CompetitionService;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.service.dto.CompetitionDTO;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;
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
    
    private final SubmissionService submissionService;

    private final UserRepository userRepository;

    @Autowired
    private ApplicationProperties applicationProperties;
    
    public CompetitionResource(CompetitionService competitionService,
    		UserRepository userRepository,
    		SubmissionService submissionService) {
        this.competitionService = competitionService;
        this.userRepository = userRepository;
        this.submissionService = submissionService;
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
        return ResponseEntity.ok(problem);
    }
    
    @PostMapping("/competitions/{id}/problem/{compProb}/submit")
    @Timed
    public ResponseEntity<Void> submitProblem(@PathVariable Long id, 
    		@PathVariable Long compProb, 
    		@RequestBody String solution) throws Exception {
        log.debug("REST request to submit solution : {}", solution);
        // TODO
        
        SubmissionDTO submission = new SubmissionDTO();
        submission.setUserId(userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin().get()).get().getId());
        submission.setCompetitionProblemId(compProb);
        submission = submissionService.save(submission);
        
        File submissionsDir = new File(applicationProperties.getWorkDir(), "submissions");
        File submissionDir = new File(submissionsDir, ""+submission.getId());
        File submissionFile = new File(submissionDir, "solution.cpp");
        
        submissionDir.mkdirs();
        FileUtils.writeStringToFile(submissionFile, solution, StandardCharsets.UTF_8);
        submission.setVerdict("waiting");
        submissionService.save(submission);
        
        return ResponseEntity.ok().build();
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
    public ResponseEntity<List<UserPoints>> getStandings(@PathVariable Long id, Pageable pageable) {
        log.debug("REST request to get standings for competition: {}", id);
		Page<UserPoints> page = competitionService.findStandings(id, pageable);
        String url = String.format("/api/competitions/{id}/standings", id);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, url);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
    
    /**
     * DELETE  /competitions/:id : delete the "id" competition.
     *
     * @param id the id of the competitionDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/competitions/{id}")
    @Timed
    public ResponseEntity<Void> deleteCompetition(@PathVariable Long id) {
        log.debug("REST request to delete Competition : {}", id);
        competitionService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
