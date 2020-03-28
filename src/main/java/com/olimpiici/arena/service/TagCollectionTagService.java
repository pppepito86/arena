package com.olimpiici.arena.service;

import com.olimpiici.arena.service.TagCollectionTagService;
import com.olimpiici.arena.domain.TagCollectionTag;
import com.olimpiici.arena.repository.TagCollectionTagRepository;
import com.olimpiici.arena.service.dto.TagCollectionTagDTO;
import com.olimpiici.arena.service.mapper.TagCollectionTagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service ementation for managing TagCollectionTag.
 */
@Service
@Transactional
public class TagCollectionTagService {

    private final Logger log = LoggerFactory.getLogger(TagCollectionTagService.class);

    private final TagCollectionTagRepository tagCollectionTagRepository;

    private final TagCollectionTagMapper tagCollectionTagMapper;

    public TagCollectionTagService(TagCollectionTagRepository tagCollectionTagRepository, TagCollectionTagMapper tagCollectionTagMapper) {
        this.tagCollectionTagRepository = tagCollectionTagRepository;
        this.tagCollectionTagMapper = tagCollectionTagMapper;
    }

    /**
     * Save a tagCollectionTag.
     *
     * @param tagCollectionTagDTO the entity to save
     * @return the persisted entity
     */
    
    public TagCollectionTagDTO save(TagCollectionTagDTO tagCollectionTagDTO) {
        log.debug("Request to save TagCollectionTag : {}", tagCollectionTagDTO);

        TagCollectionTag tagCollectionTag = tagCollectionTagMapper.toEntity(tagCollectionTagDTO);
        tagCollectionTag = tagCollectionTagRepository.save(tagCollectionTag);
        return tagCollectionTagMapper.toDto(tagCollectionTag);
    }

    /**
     * Get all the tagCollectionTags.
     *
     * @return the list of entities
     */
    
    @Transactional(readOnly = true)
    public List<TagCollectionTagDTO> findAll() {
        log.debug("Request to get all TagCollectionTags");
        return tagCollectionTagRepository.findAll().stream()
            .map(tagCollectionTagMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }


    /**
     * Get one tagCollectionTag by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    
    @Transactional(readOnly = true)
    public Optional<TagCollectionTagDTO> findOne(Long id) {
        log.debug("Request to get TagCollectionTag : {}", id);
        return tagCollectionTagRepository.findById(id)
            .map(tagCollectionTagMapper::toDto);
    }

    /**
     * Delete the tagCollectionTag by id.
     *
     * @param id the id of the entity
     */
    
    public void delete(Long id) {
        log.debug("Request to delete TagCollectionTag : {}", id);
        tagCollectionTagRepository.deleteById(id);
    }  
}
