package com.olimpiici.arena.service;

import com.olimpiici.arena.service.dto.TagDTO;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing Tag.
 */
public interface TagService {

    /**
     * Save a tag.
     *
     * @param tagDTO the entity to save
     * @return the persisted entity
     */
    TagDTO save(TagDTO tagDTO);

    /**
     * Get all the tags.
     *
     * @return the list of entities
     */
    List<TagDTO> findAll();


    /**
     * Get the "id" tag.
     *
     * @param id the id of the entity
     * @return the entity
     */
    Optional<TagDTO> findOne(Long id);

    /**
     * Delete the "id" tag.
     *
     * @param id the id of the entity
     */
    void delete(Long id);
}
