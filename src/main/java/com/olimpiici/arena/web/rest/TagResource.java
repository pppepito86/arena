package com.olimpiici.arena.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.security.SecurityUtils;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.ProblemService;
import com.olimpiici.arena.service.TagService;
import com.olimpiici.arena.web.rest.errors.BadRequestAlertException;
import com.olimpiici.arena.web.rest.util.HeaderUtil;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.service.mapper.CompetitionProblemMapper;

import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing Tag.
 */
@RestController
@RequestMapping("/api")
public class TagResource {

    private final Logger log = LoggerFactory.getLogger(TagResource.class);

    private static final String ENTITY_NAME = "tag";

    private final TagService tagService;

    private final CompetitionProblemService competitionProblemService;
    
    private final ProblemService problemService;
    
    private final CompetitionProblemMapper competitionProblemMapper;
    
    public TagResource(TagService tagService,
    		CompetitionProblemService competitionProblemService,
    		ProblemService problemService,
    		CompetitionProblemMapper competitionProblemMapper) {
        this.tagService = tagService;
        this.competitionProblemService = competitionProblemService;
        this.competitionProblemMapper = competitionProblemMapper;
        this.problemService = problemService;
    }

    /**
     * POST  /tags : Create a new tag.
     *
     * @param tagDTO the tagDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new tagDTO, or with status 400 (Bad Request) if the tag has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    @PostMapping("/tags")
    @Timed
    public ResponseEntity<TagDTO> createTag(@RequestBody TagDTO tagDTO) throws URISyntaxException {
        log.debug("REST request to save Tag : {}", tagDTO);
        if (tagDTO.getId() != null) {
            throw new BadRequestAlertException("A new tag cannot already have an ID", ENTITY_NAME, "idexists");
        }
        TagDTO result = tagService.save(tagDTO);
        return ResponseEntity.created(new URI("/api/tags/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /tags : Updates an existing tag.
     *
     * @param tagDTO the tagDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated tagDTO,
     * or with status 400 (Bad Request) if the tagDTO is not valid,
     * or with status 500 (Internal Server Error) if the tagDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    @PutMapping("/tags")
    @Timed
    public ResponseEntity<TagDTO> updateTag(@RequestBody TagDTO tagDTO) throws URISyntaxException {
        log.debug("REST request to update Tag : {}", tagDTO);
        if (tagDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        TagDTO result = tagService.save(tagDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, tagDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /tags : get all the tags.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of tags in body
     */
    @GetMapping("/tags")
    @Timed
    public List<TagDTO> getAllTags(@RequestParam("publicOnly") Optional<Boolean> publicOnly) {
    	log.debug("REST request to get all Tags");
    	boolean isAdmin = SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN);
    	if(!isAdmin || (publicOnly.isPresent() && publicOnly.get() == true)) {
    		return tagService.findAllPublic();
    	} else {
    		return tagService.findAll();
    	}
    }

    /**
     * GET  /tags/:id : get the "id" tag.
     *
     * @param id the id of the tagDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the tagDTO, or with status 404 (Not Found)
     */
    @GetMapping("/tags/{id}")
    @Timed
    public ResponseEntity<TagDTO> getTag(@PathVariable Long id) {
        log.debug("REST request to get Tag : {}", id);
        Optional<TagDTO> tagDTO = tagService.findOne(id);
        return ResponseUtil.wrapOrNotFound(tagDTO);
    }
    
    @GetMapping("/tags/{id}/problems")
    @Timed
    public List<CompetitionProblemDTO> getTagProblem(@PathVariable Long id) {
        log.debug("REST request to get Tag : {}", id);
        
        List<ProblemDTO> problems = tagService.problemsForTag(id);
        Map<Long, ProblemDTO> idToProblem = new HashMap<>();;
        problems.stream()
        	.forEach(problem -> idToProblem.put(problem.getId(), problem));
        
        List<CompetitionProblemDTO> problemsDTO = problems  
        		.stream()
        		.map(cp -> competitionProblemService.findOneByProblem(cp.getId()))
        		.filter(optional -> optional.isPresent())
        		.map(optional -> optional.get())
        		.map(dto -> {
        			dto.setTitle(idToProblem.get(dto.getProblemId()).getTitle());
        			return dto;
        		}).collect(Collectors.toList());
        
        return problemsDTO;
    }
    
    @GetMapping("/tags/{id}/submissions")
    @Timed
    public List<SubmissionDTO> getTagSubmissions(@PathVariable Long id) {
        log.debug("REST request to get Tag : {}", id);
        return tagService.submissionsForTag(id);
    }
    
    /**
     * Returns problem which don't have an official tag with id {id}, but 
     * have a submission tagged with {id}. 
     * @param id
     * @return
     */
    @GetMapping("/tags/{id}/problems-tagged-by-users")
    @Timed
    public List<CompetitionProblemDTO> getProblemTaggedOnlyByUser(@PathVariable Long id) {
        log.debug("REST request to get Tag : {}", id);
        return tagService.problemsTaggedByUsers(id)
        		.map(competitionProblem -> {
        			CompetitionProblemDTO dto = competitionProblemMapper.toDto(competitionProblem);
        			dto.setTitle(competitionProblem.getProblem().getTitle());
        			return dto;
        		})
        		.collect(Collectors.toList());
    }
    

    /**
     * DELETE  /tags/:id : delete the "id" tag.
     *
     * @param id the id of the tagDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    @DeleteMapping("/tags/{id}")
    @Timed
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        log.debug("REST request to delete Tag : {}", id);
        tagService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
