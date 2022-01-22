package com.olimpiici.arena.web.rest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.codahale.metrics.annotation.Timed;
import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.grader.WorkerPool;
import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.ProblemService;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.web.rest.errors.BadRequestAlertException;
import com.olimpiici.arena.web.rest.util.HeaderUtil;
import com.olimpiici.arena.web.rest.util.PaginationUtil;

import io.github.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing Problem.
 */
@RestController
@RequestMapping("/api")
public class ProblemResource {

    private final Logger log = LoggerFactory.getLogger(ProblemResource.class);

    private static final String ENTITY_NAME = "problem";

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private WorkerPool workerPool;

    private final ProblemService problemService;

    private final CompetitionProblemService competitionProblemService;

    public ProblemResource(ProblemService problemService, CompetitionProblemService competitionProblemService) {
        this.problemService = problemService;
        this.competitionProblemService = competitionProblemService;
    }

    /**
     * POST  /problems : Create a new problem.
     *
     * @param problemDTO the problemDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new problemDTO, or with status 400 (Bad Request) if the problem has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/problems")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ProblemDTO> createProblem(@RequestBody ProblemDTO problemDTO) throws URISyntaxException {
        log.debug("REST request to save Problem : {}", problemDTO);
        if (problemDTO.getId() != null) {
            throw new BadRequestAlertException("A new problem cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ProblemDTO result = problemService.save(problemDTO);
        return ResponseEntity.created(new URI("/api/problems/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /problems : Updates an existing problem.
     *
     * @param problemDTO the problemDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated problemDTO,
     * or with status 400 (Bad Request) if the problemDTO is not valid,
     * or with status 500 (Internal Server Error) if the problemDTO couldn't be updated
     * @throws Exception
     */
    @PutMapping("/problems")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<ProblemDTO> updateProblem(@RequestBody ProblemDTO problemDTO) throws Exception {
        log.debug("REST request to update Problem : {}", problemDTO);
        Long problemId = problemDTO.getId();
		if (problemId == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        ProblemDTO result = problemService.save(problemDTO);
        problemService.updateMemoryLimit(problemId, problemDTO.getMemory());
        problemService.updateTimeLimit(problemId, problemDTO.getTime());

        competitionProblemService.findOneByProblem(problemId)
        		.ifPresent(cp -> {
					try {
						competitionProblemService.submitAuthorsIfNeeded(cp.getId());
					} catch (IOException e) {
						e.printStackTrace();
					}
				});

        workerPool.deleteProblem(problemId);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, problemId.toString()))
            .body(result);
    }


    /**
     * GET  /problems : get all the problems.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of problems in body
     */
    @GetMapping("/problems")
    @Timed
    public ResponseEntity<List<ProblemDTO>> getAllProblems(Pageable pageable) {
        log.debug("REST request to get a page of Problems");

        Page<ProblemDTO> page = problemService.findAll(pageable);
        for (ProblemDTO problemDTO : page) {
			problemDTO.allTags = problemService.findTags(problemDTO.getId());
		}
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/problems");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET  /problems/:id : get the "id" problem.
     *
     * @param id the id of the problemDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the problemDTO, or with status 404 (Not Found)
     */
    @GetMapping("/problems/{id}")
    @Timed
    public ResponseEntity<ProblemDTO> getProblem(@PathVariable Long id) {
        log.debug("REST request to get Problem : {}", id);
        Optional<ProblemDTO> problemDTO = problemService.findOne(id)
        		.map(dto -> problemService.setLimitsToDto(dto));
        return ResponseUtil.wrapOrNotFound(problemDTO);
    }

    @PostMapping("/problems/{id}/zip")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<?> updateZip(@PathVariable Long id, @RequestBody MultipartFile file)
    		throws Exception {
        log.debug("REST updating zip for: {} {}", id, file.getOriginalFilename());

        File zipFile = Paths.get(applicationProperties.getWorkDir(), "problems", ""+id, "problem.zip").toFile();
        FileUtils.copyInputStreamToFile(file.getInputStream(), zipFile);
        problemService.unzipProblemZip(id);

        workerPool.deleteProblem(id);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/problems/{id}/tags")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<?> updateTags(@PathVariable Long id, @RequestBody List<TagDTO> tags)
    		throws URISyntaxException {
        log.debug("REST updating tags for: {} {}", id, tags);
        problemService.updateTags(id, tags);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/problems/{id}/tags")
    @Timed
    public List<TagDTO> getTags(@PathVariable Long id)
    		throws URISyntaxException {
        log.debug("REST getting tags for: {}", id);
        return problemService.findTags(id);
    }

    /**
     * DELETE  /problems/:id : delete the "id" problem.
     *
     * @param id the id of the problemDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/problems/{id}")
    @Timed
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteProblem(@PathVariable Long id) {
        log.debug("REST request to delete Problem : {}", id);
        problemService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
