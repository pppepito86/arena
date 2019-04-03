package com.olimpiici.arena.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.security.SecurityUtils;
import com.olimpiici.arena.service.ProblemService;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.service.util.RandomUtil;
import com.olimpiici.arena.web.rest.errors.BadRequestAlertException;
import com.olimpiici.arena.web.rest.util.HeaderUtil;
import com.olimpiici.arena.web.rest.util.PaginationUtil;

import io.github.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing Submission.
 */
@RestController
@RequestMapping("/api")
public class SubmissionResource {

    private final Logger log = LoggerFactory.getLogger(SubmissionResource.class);

    private static final String ENTITY_NAME = "submission";

    @Autowired
    private ApplicationProperties applicationProperties;
    
    private final SubmissionService submissionService;
    
    private final ProblemService problemService;
    
    private final UserRepository userRepository;

    public SubmissionResource(SubmissionService submissionService,
    	    ProblemService problemService,
    		UserRepository userRepository) {
        this.submissionService = submissionService;
        this.problemService = problemService;
        this.userRepository = userRepository;
    }

    /**
     * POST  /submissions : Create a new submission.
     *
     * @param submissionDTO the submissionDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new submissionDTO, or with status 400 (Bad Request) if the submission has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/submissions")
    @Timed
    public ResponseEntity<SubmissionDTO> createSubmission(@RequestBody SubmissionDTO submissionDTO) throws URISyntaxException {
        log.debug("REST request to save Submission : {}", submissionDTO);
        if (submissionDTO.getId() != null) {
            throw new BadRequestAlertException("A new submission cannot already have an ID", ENTITY_NAME, "idexists");
        }
        submissionDTO.setUploadDate(ZonedDateTime.now());
        submissionDTO.setSecurityKey(RandomUtil.generateSubmissionSecurityKey());
        SubmissionDTO result = submissionService.save(submissionDTO);
        return ResponseEntity.created(new URI("/api/submissions/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /submissions : Updates an existing submission.
     *
     * @param submissionDTO the submissionDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated submissionDTO,
     * or with status 400 (Bad Request) if the submissionDTO is not valid,
     * or with status 500 (Internal Server Error) if the submissionDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/submissions")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<SubmissionDTO> updateSubmission(@RequestBody SubmissionDTO submissionDTO) throws URISyntaxException {
        log.debug("REST request to update Submission : {}", submissionDTO);
        if (submissionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        SubmissionDTO result = submissionService.save(submissionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, submissionDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /submissions : get all the submissions.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of submissions in body
     */
    @GetMapping("/submissions")
    @Timed
    public ResponseEntity<List<SubmissionDTO>> getAllSubmissions(Pageable pageable) {
        log.debug("REST request to get a page of Submissions");
        
        boolean isAdmin = SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN);
        
        Page<SubmissionDTO> page;
        if (isAdmin) {
        	page = submissionService.findAll(pageable);
        } else {
        	User user = userRepository
        			.findOneByLogin(SecurityUtils.getCurrentUserLogin().get()).get();
        	page = submissionService.findByUser(user, pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/submissions");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET  /submissions/:id : get the "id" submission.
     *
     * @param id the id of the submissionDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the submissionDTO, or with status 404 (Not Found)
     */
    @GetMapping("/submissions/{id}")
    @Timed
    public ResponseEntity<SubmissionDTO> getSubmission(@PathVariable Long id,
    		@RequestParam(value = "securityKey", defaultValue = "") String securityKey) {
        log.debug("REST request to get Submission : {} with security key {} ", id, securityKey);
        
        boolean isAdmin = SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN);
        
        User user = 
    			userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin().get()).get();
        Optional<SubmissionDTO> submissionDTO = submissionService.findOne(id);
        if (!submissionDTO.isPresent()) {
        	ResponseUtil.wrapOrNotFound(submissionDTO); 
        }
        
        boolean isSubmissionAuthor = submissionDTO.get().getUserId().equals(user.getId());
        boolean goodSecurityCode; 
        if (submissionDTO.get().getSecurityKey() == null) { 
        	goodSecurityCode = false;
        } else {
        	goodSecurityCode = submissionDTO.get().getSecurityKey().equals(securityKey);
        }
        
        if (isAdmin || isSubmissionAuthor || goodSecurityCode) {
        	return ResponseUtil.wrapOrNotFound(submissionDTO);
        } else {
        	return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * DELETE  /submissions/:id : delete the "id" submission.
     *
     * @param id the id of the submissionDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/submissions/{id}")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        log.debug("REST request to delete Submission : {}", id);
        submissionService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
    
    @PostMapping("/submissions/{id}/tags")
    @Timed
    public ResponseEntity updateTags(@PathVariable Long id, @RequestBody List<TagDTO> tags)
    		throws URISyntaxException {
        log.debug("REST updating tags for: {} {}", id, tags);
        submissionService.updateTags(id, tags);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/submissions/{id}/tags")
    @Timed
    public List<TagDTO> getTags(@PathVariable Long id)
    		throws URISyntaxException {
        log.debug("REST getting tags for: {}", id);
        return submissionService.findTags(id);
    }

}
