package com.olimpiici.arena.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.olimpiici.arena.service.TagCollectionTagService;
import com.olimpiici.arena.web.rest.errors.BadRequestAlertException;
import com.olimpiici.arena.web.rest.util.HeaderUtil;
import com.olimpiici.arena.service.dto.TagCollectionTagDTO;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing TagCollectionTag.
 */
@RestController
@RequestMapping("/api")
public class TagCollectionTagResource {

    private final Logger log = LoggerFactory.getLogger(TagCollectionTagResource.class);

    private static final String ENTITY_NAME = "tagCollectionTag";

    private final TagCollectionTagService tagCollectionTagService;

    public TagCollectionTagResource(TagCollectionTagService tagCollectionTagService) {
        this.tagCollectionTagService = tagCollectionTagService;
    }

    /**
     * POST  /tag-collection-tags : Create a new tagCollectionTag.
     *
     * @param tagCollectionTagDTO the tagCollectionTagDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new tagCollectionTagDTO, or with status 400 (Bad Request) if the tagCollectionTag has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/tag-collection-tags")
    @Timed
    public ResponseEntity<TagCollectionTagDTO> createTagCollectionTag(@RequestBody TagCollectionTagDTO tagCollectionTagDTO) throws URISyntaxException {
        log.debug("REST request to save TagCollectionTag : {}", tagCollectionTagDTO);
        if (tagCollectionTagDTO.getId() != null) {
            throw new BadRequestAlertException("A new tagCollectionTag cannot already have an ID", ENTITY_NAME, "idexists");
        }
        TagCollectionTagDTO result = tagCollectionTagService.save(tagCollectionTagDTO);
        return ResponseEntity.created(new URI("/api/tag-collection-tags/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /tag-collection-tags : Updates an existing tagCollectionTag.
     *
     * @param tagCollectionTagDTO the tagCollectionTagDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated tagCollectionTagDTO,
     * or with status 400 (Bad Request) if the tagCollectionTagDTO is not valid,
     * or with status 500 (Internal Server Error) if the tagCollectionTagDTO couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/tag-collection-tags")
    @Timed
    public ResponseEntity<TagCollectionTagDTO> updateTagCollectionTag(@RequestBody TagCollectionTagDTO tagCollectionTagDTO) throws URISyntaxException {
        log.debug("REST request to update TagCollectionTag : {}", tagCollectionTagDTO);
        if (tagCollectionTagDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        TagCollectionTagDTO result = tagCollectionTagService.save(tagCollectionTagDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, tagCollectionTagDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /tag-collection-tags : get all the tagCollectionTags.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of tagCollectionTags in body
     */
    @GetMapping("/tag-collection-tags")
    @Timed
    public List<TagCollectionTagDTO> getAllTagCollectionTags() {
        log.debug("REST request to get all TagCollectionTags");
        return tagCollectionTagService.findAll();
    }

    /**
     * GET  /tag-collection-tags/:id : get the "id" tagCollectionTag.
     *
     * @param id the id of the tagCollectionTagDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the tagCollectionTagDTO, or with status 404 (Not Found)
     */
    @GetMapping("/tag-collection-tags/{id}")
    @Timed
    public ResponseEntity<TagCollectionTagDTO> getTagCollectionTag(@PathVariable Long id) {
        log.debug("REST request to get TagCollectionTag : {}", id);
        Optional<TagCollectionTagDTO> tagCollectionTagDTO = tagCollectionTagService.findOne(id);
        return ResponseUtil.wrapOrNotFound(tagCollectionTagDTO);
    }

    /**
     * DELETE  /tag-collection-tags/:id : delete the "id" tagCollectionTag.
     *
     * @param id the id of the tagCollectionTagDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/tag-collection-tags/{id}")
    @Timed
    public ResponseEntity<Void> deleteTagCollectionTag(@PathVariable Long id) {
        log.debug("REST request to delete TagCollectionTag : {}", id);
        tagCollectionTagService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
