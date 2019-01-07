package com.olimpiici.arena.service;

import com.olimpiici.arena.service.dto.TagCollectionTagDTO;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing TagCollectionTag.
 */
public interface TagCollectionTagService {

    /**
     * Save a tagCollectionTag.
     *
     * @param tagCollectionTagDTO the entity to save
     * @return the persisted entity
     */
    TagCollectionTagDTO save(TagCollectionTagDTO tagCollectionTagDTO);

    /**
     * Get all the tagCollectionTags.
     *
     * @return the list of entities
     */
    List<TagCollectionTagDTO> findAll();


    /**
     * Get the "id" tagCollectionTag.
     *
     * @param id the id of the entity
     * @return the entity
     */
    Optional<TagCollectionTagDTO> findOne(Long id);

    /**
     * Delete the "id" tagCollectionTag.
     *
     * @param id the id of the entity
     */
    void delete(Long id);
}
