package com.olimpiici.arena.service;

import com.olimpiici.arena.service.TagCollectionService;
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.repository.TagCollectionRepository;
import com.olimpiici.arena.service.dto.TagCollectionDTO;
import com.olimpiici.arena.service.mapper.TagCollectionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service ementation for managing TagCollection.
 */
@Service
@Transactional
public class TagCollectionService {

    private final Logger log = LoggerFactory.getLogger(TagCollectionService.class);

    private final TagCollectionRepository tagCollectionRepository;

    private final TagCollectionMapper tagCollectionMapper;

    public TagCollectionService(TagCollectionRepository tagCollectionRepository, TagCollectionMapper tagCollectionMapper) {
        this.tagCollectionRepository = tagCollectionRepository;
        this.tagCollectionMapper = tagCollectionMapper;
    }

    /**
     * Save a tagCollection.
     *
     * @param tagCollectionDTO the entity to save
     * @return the persisted entity
     */
    
    public TagCollectionDTO save(TagCollectionDTO tagCollectionDTO) {
        log.debug("Request to save TagCollection : {}", tagCollectionDTO);

        TagCollection tagCollection = tagCollectionMapper.toEntity(tagCollectionDTO);
        tagCollection = tagCollectionRepository.save(tagCollection);
        return tagCollectionMapper.toDto(tagCollection);
    }

    /**
     * Get all the tagCollections.
     *
     * @return the list of entities
     */
    
    @Transactional(readOnly = true)
    public List<TagCollectionDTO> findAll() {
        log.debug("Request to get all TagCollections");
        return tagCollectionRepository.findAll().stream()
            .map(tagCollectionMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }


    /**
     * Get one tagCollection by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    
    @Transactional(readOnly = true)
    public Optional<TagCollectionDTO> findOne(Long id) {
        log.debug("Request to get TagCollection : {}", id);
        return tagCollectionRepository.findById(id)
            .map(tagCollectionMapper::toDto);
    }

    /**
     * Delete the tagCollection by id.
     *
     * @param id the id of the entity
     */
    
    public void delete(Long id) {
        log.debug("Request to delete TagCollection : {}", id);
        tagCollectionRepository.deleteById(id);
    }
}
