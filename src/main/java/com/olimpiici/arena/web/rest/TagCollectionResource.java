package com.olimpiici.arena.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.service.TagCollectionService;
import com.olimpiici.arena.web.rest.errors.BadRequestAlertException;
import com.olimpiici.arena.web.rest.util.HeaderUtil;
import com.olimpiici.arena.service.dto.TagCollectionDTO;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing TagCollection.
 */
@RestController
@RequestMapping("/api")
public class TagCollectionResource {

    private final Logger log = LoggerFactory.getLogger(TagCollectionResource.class);

    private static final String ENTITY_NAME = "tagCollection";

    private final TagCollectionService tagCollectionService;

    public TagCollectionResource(TagCollectionService tagCollectionService) {
        this.tagCollectionService = tagCollectionService;
    }

    /**
     * POST  /tag-collections : Create a new tagCollection.
     *
     * @param tagCollectionDTO the tagCollectionDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new tagCollectionDTO, or with status 400 (Bad Request) if the tagCollection has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    @PostMapping("/tag-collections")
    @Timed
    public ResponseEntity<TagCollectionDTO> createTagCollection(@RequestBody TagCollectionDTO tagCollectionDTO) throws URISyntaxException {
        log.debug("REST request to save TagCollection : {}", tagCollectionDTO);
        if (tagCollectionDTO.getId() != null) {
            throw new BadRequestAlertException("A new tagCollection cannot already have an ID", ENTITY_NAME, "idexists");
        }
        TagCollectionDTO result = tagCollectionService.save(tagCollectionDTO);
        return ResponseEntity.created(new URI("/api/tag-collections/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /tag-collections : Updates an existing tagCollection.
     *
     * @param tagCollectionDTO the tagCollectionDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated tagCollectionDTO,
     * or with status 400 (Bad Request) if the tagCollectionDTO is not valid,
     * or with status 500 (Internal Server Error) if the tagCollectionDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    @PutMapping("/tag-collections")
    @Timed
    public ResponseEntity<TagCollectionDTO> updateTagCollection(@RequestBody TagCollectionDTO tagCollectionDTO) throws URISyntaxException {
        log.debug("REST request to update TagCollection : {}", tagCollectionDTO);
        if (tagCollectionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        TagCollectionDTO result = tagCollectionService.save(tagCollectionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, tagCollectionDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /tag-collections : get all the tagCollections.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of tagCollections in body
     */
    @GetMapping("/tag-collections")
    @Timed
    public List<TagCollectionDTO> getAllTagCollections() {
        log.debug("REST request to get all TagCollections");
        return tagCollectionService.findAll();
    }

    /**
     * GET  /tag-collections/:id : get the "id" tagCollection.
     *
     * @param id the id of the tagCollectionDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the tagCollectionDTO, or with status 404 (Not Found)
     */
    @GetMapping("/tag-collections/{id}")
    @Timed
    public ResponseEntity<TagCollectionDTO> getTagCollection(@PathVariable Long id) {
        log.debug("REST request to get TagCollection : {}", id);
        Optional<TagCollectionDTO> tagCollectionDTO = tagCollectionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(tagCollectionDTO);
    }

    /**
     * DELETE  /tag-collections/:id : delete the "id" tagCollection.
     *
     * @param id the id of the tagCollectionDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    @DeleteMapping("/tag-collections/{id}")
    @Timed
    public ResponseEntity<Void> deleteTagCollection(@PathVariable Long id) {
        log.debug("REST request to delete TagCollection : {}", id);
        tagCollectionService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
