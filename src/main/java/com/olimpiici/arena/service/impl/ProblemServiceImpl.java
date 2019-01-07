package com.olimpiici.arena.service.impl;

import com.olimpiici.arena.service.ProblemService;
import com.olimpiici.arena.service.TagService;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Tag;
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.domain.TagCollectionTag;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.repository.TagCollectionRepository;
import com.olimpiici.arena.repository.TagCollectionTagRepository;
import com.olimpiici.arena.repository.TagRepository;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.service.mapper.ProblemMapper;
import com.olimpiici.arena.service.mapper.TagMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service Implementation for managing Problem.
 */
@Service
@Transactional
public class ProblemServiceImpl implements ProblemService {

    private final Logger log = LoggerFactory.getLogger(ProblemServiceImpl.class);

    private final ProblemRepository problemRepository;

    private final ProblemMapper problemMapper;
    
    private final TagCollectionTagRepository tagCollectionTagRepository;
    
    private final TagCollectionRepository tagCollectionRepository;

    private final TagRepository tagRepository;
    
    private final TagMapper tagMapper;
    
    private final TagService tagService;
    
    
    public ProblemServiceImpl(ProblemRepository problemRepository, 
    		ProblemMapper problemMapper, 
    		TagCollectionTagRepository tagCollectionTagRepository,
    		TagCollectionRepository tagCollectionRepository,
    		TagRepository tagRepository,
    		TagMapper tagMapper,
    		TagService tagService) {
        this.problemRepository = problemRepository;
        this.problemMapper = problemMapper;
        this.tagCollectionTagRepository = tagCollectionTagRepository;
        this.tagCollectionRepository = tagCollectionRepository;
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
        this.tagService = tagService;
    }

    /**
     * Save a problem.
     *
     * @param problemDTO the entity to save
     * @return the persisted entity
     */
    @Override
    public ProblemDTO save(ProblemDTO problemDTO) {
        log.debug("Request to save Problem : {}", problemDTO);

        Problem problem = problemMapper.toEntity(problemDTO);
        problem = problemRepository.save(problem);
        return problemMapper.toDto(problem);
    }

    /**
     * Get all the problems.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProblemDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Problems");
        return problemRepository.findAll(pageable)
            .map(problemMapper::toDto);
    }


    /**
     * Get one problem by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ProblemDTO> findOne(Long id) {
        log.debug("Request to get Problem : {}", id);
        return problemRepository.findById(id)
            .map(problemMapper::toDto);
    }

    /**
     * Delete the problem by id.
     *
     * @param id the id of the entity
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete Problem : {}", id);
        problemRepository.deleteById(id);
    }
    
    @Override
    public List<TagDTO> findTags(Long id) {
    	Problem problem = problemRepository.getOne(id);
    	return tagService.findTagsForCollection(problem.getTags())
	    	.map(tagMapper::toDto)
			.collect(Collectors.toList());
    }
    
    @Override
    public void updateTags(Long id, List<TagDTO> newTags) {
    	Problem problem = problemRepository.getOne(id);
    	TagCollection newCollection	= 
    			tagService.updateTagsForCollection(problem.getTags(), newTags);
    	
    	if (problem.getTags() == null) {
	    	problem.setTags(newCollection);
			problemRepository.save(problem);
    	}
    }
}
