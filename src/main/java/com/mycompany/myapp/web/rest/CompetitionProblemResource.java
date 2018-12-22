package com.mycompany.myapp.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.mycompany.myapp.service.CompetitionProblemService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import com.mycompany.myapp.web.rest.util.HeaderUtil;
import com.mycompany.myapp.web.rest.util.PaginationUtil;
import com.mycompany.myapp.service.dto.CompetitionProblemDTO;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing CompetitionProblem.
 */
@RestController
@RequestMapping("/api")
public class CompetitionProblemResource {

    private final Logger log = LoggerFactory.getLogger(CompetitionProblemResource.class);

    private static final String ENTITY_NAME = "competitionProblem";

    private final CompetitionProblemService competitionProblemService;

    public CompetitionProblemResource(CompetitionProblemService competitionProblemService) {
        this.competitionProblemService = competitionProblemService;
    }

    /**
     * POST  /competition-problems : Create a new competitionProblem.
     *
     * @param competitionProblemDTO the competitionProblemDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new competitionProblemDTO, or with status 400 (Bad Request) if the competitionProblem has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/competition-problems")
    @Timed
    public ResponseEntity<CompetitionProblemDTO> createCompetitionProblem(@RequestBody CompetitionProblemDTO competitionProblemDTO) throws URISyntaxException {
        log.debug("REST request to save CompetitionProblem : {}", competitionProblemDTO);
        if (competitionProblemDTO.getId() != null) {
            throw new BadRequestAlertException("A new competitionProblem cannot already have an ID", ENTITY_NAME, "idexists");
        }
        CompetitionProblemDTO result = competitionProblemService.save(competitionProblemDTO);
        return ResponseEntity.created(new URI("/api/competition-problems/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /competition-problems : Updates an existing competitionProblem.
     *
     * @param competitionProblemDTO the competitionProblemDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated competitionProblemDTO,
     * or with status 400 (Bad Request) if the competitionProblemDTO is not valid,
     * or with status 500 (Internal Server Error) if the competitionProblemDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/competition-problems")
    @Timed
    public ResponseEntity<CompetitionProblemDTO> updateCompetitionProblem(@RequestBody CompetitionProblemDTO competitionProblemDTO) throws URISyntaxException {
        log.debug("REST request to update CompetitionProblem : {}", competitionProblemDTO);
        if (competitionProblemDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        CompetitionProblemDTO result = competitionProblemService.save(competitionProblemDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, competitionProblemDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /competition-problems : get all the competitionProblems.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of competitionProblems in body
     */
    @GetMapping("/competition-problems")
    @Timed
    public ResponseEntity<List<CompetitionProblemDTO>> getAllCompetitionProblems(Pageable pageable) {
        log.debug("REST request to get a page of CompetitionProblems");
        Page<CompetitionProblemDTO> page = competitionProblemService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/competition-problems");
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * GET  /competition-problems/:id : get the "id" competitionProblem.
     *
     * @param id the id of the competitionProblemDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the competitionProblemDTO, or with status 404 (Not Found)
     */
    @GetMapping("/competition-problems/{id}")
    @Timed
    public ResponseEntity<CompetitionProblemDTO> getCompetitionProblem(@PathVariable Long id) {
        log.debug("REST request to get CompetitionProblem : {}", id);
        Optional<CompetitionProblemDTO> competitionProblemDTO = competitionProblemService.findOne(id);
        return ResponseUtil.wrapOrNotFound(competitionProblemDTO);
    }

    /**
     * DELETE  /competition-problems/:id : delete the "id" competitionProblem.
     *
     * @param id the id of the competitionProblemDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/competition-problems/{id}")
    @Timed
    public ResponseEntity<Void> deleteCompetitionProblem(@PathVariable Long id) {
        log.debug("REST request to delete CompetitionProblem : {}", id);
        competitionProblemService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
