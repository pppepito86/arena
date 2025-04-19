package com.olimpiici.arena.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.security.SecurityUtils;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.CompetitionService;
import com.olimpiici.arena.service.TagService;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.service.mapper.CompetitionProblemMapper;
import com.olimpiici.arena.web.rest.errors.BadRequestAlertException;
import com.olimpiici.arena.web.rest.util.HeaderUtil;

import io.github.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing Tag.
 */
@RestController
@RequestMapping("/api")
public class TagResource {

    private final Logger log = LoggerFactory.getLogger(TagResource.class);

    private static final String ENTITY_NAME = "tag";

    @Autowired
    private TagService tagService;

    @Autowired
    private CompetitionProblemService competitionProblemService;

    @Autowired
    private CompetitionProblemMapper competitionProblemMapper;

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private UserRepository userRepository;

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
        Map<Long, ProblemDTO> idToProblem = new HashMap<>();
        problems.stream()
        	.forEach(problem -> idToProblem.put(problem.getId(), problem));

        List<CompetitionProblemDTO> problemsDTO = problems
            .stream()
            .flatMap(problem -> competitionProblemService.findAllByProblem(problem.getId()).stream())
            .map(dto -> {
                dto.setTitle(idToProblem.get(dto.getProblemId()).getTitle());
                if (dto.getCompetitionId() != null) {
                dto.path = competitionService.findPathFromRoot(dto.getCompetitionId())
                    .stream()
                    .map(comp -> comp.getLabel())
                    .collect(Collectors.toList());
                }
                return dto;
            }).collect(Collectors.toList());
        
        addPointsToDto(problemsDTO);

        return problemsDTO;
    }

    private void addPointsToDto(List<CompetitionProblemDTO> problemsDTO) {
        List<Long> compProblemIds = problemsDTO.stream()
                .map(cp -> cp.getId())
                .collect(Collectors.toList());
        Long userId = userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin().get()).get().getId();
        Map<Long, Integer> pointsPerProblem = competitionService.findSimplePointsForUserPerProblem(compProblemIds, userId);
        for (CompetitionProblemDTO dto : problemsDTO) {
            dto.setPoints(pointsPerProblem.get(dto.getId()));
        }
    }

    @GetMapping("/tags/{id}/submissions")
    @Timed
    public List<SubmissionDTO> getTagSubmissions(@PathVariable Long id) {
        log.debug("REST request to get Tag : {}", id);
        return tagService.submissionsForTag(id);
    }

    /**
     * Returns problem which have a submission tagged with {id}.
     * @param id
     * @return
     */
    @GetMapping("/tags/{id}/problems-tagged-by-users")
    @Timed
    public List<CompetitionProblemDTO> getProblemTaggedByUser(@PathVariable Long id) {
        log.debug("REST request to get Tag : {}", id);

        List<CompetitionProblemDTO> problemsDTO = tagService
                .problemsTaggedByUsers(id)
                .map(competitionProblem -> {
        			CompetitionProblemDTO dto = competitionProblemMapper.toDto(competitionProblem);
        			dto.setTitle(competitionProblem.getProblem().getTitle());
                    if (dto.getCompetitionId() != null) {
                        dto.path = competitionService.findPathFromRoot(dto.getCompetitionId())
                                .stream()
                                .map(comp -> comp.getLabel())
                                .collect(Collectors.toList());
                    }
        			return dto;
        		})
        		.collect(Collectors.toList());

        addPointsToDto(problemsDTO);
        return problemsDTO;
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
