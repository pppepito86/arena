package com.olimpiici.arena.service;

import com.olimpiici.arena.service.TagService;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.Tag;
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.domain.TagCollectionTag;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.TagCollectionRepository;
import com.olimpiici.arena.repository.TagCollectionTagRepository;
import com.olimpiici.arena.repository.TagRepository;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.service.mapper.ProblemMapper;
import com.olimpiici.arena.service.mapper.SubmissionMapper;
import com.olimpiici.arena.service.mapper.TagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service ementation for managing Tag.
 */
@Service
@Transactional
public class TagService {

    private final Logger log = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;

    private final TagMapper tagMapper;
    
    private final TagCollectionTagRepository tagCollectionTagRepository;
    
    private final ProblemRepository problemRepository;

    private final ProblemMapper problemMapper;
    
    private final TagCollectionRepository tagCollectionRepository;
    
    private final SubmissionRepository submissionRepository;
    
    private final SubmissionMapper submissionMapper;
    
    public TagService(TagRepository tagRepository, 
    		TagCollectionTagRepository tagCollectionTagRepository, 
    		TagCollectionRepository tagCollectionRepository,
    		ProblemRepository problemRepository,
    		TagMapper tagMapper,
    		ProblemMapper problemMapper,
    		SubmissionRepository submissionRepository,
    		SubmissionMapper submissionMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
        this.tagCollectionTagRepository = tagCollectionTagRepository;
        this.problemRepository = problemRepository;
        this.problemMapper = problemMapper;
        this.tagCollectionRepository = tagCollectionRepository;
        this.submissionRepository = submissionRepository;
        this.submissionMapper = submissionMapper;
    }

    /**
     * Save a tag.
     *
     * @param tagDTO the entity to save
     * @return the persisted entity
     */
    
    public TagDTO save(TagDTO tagDTO) {
        log.debug("Request to save Tag : {}", tagDTO);

        Tag tag = tagMapper.toEntity(tagDTO);
        tag = tagRepository.save(tag);
        return tagMapper.toDto(tag);
    }

    /**
     * Get all the tags.
     *
     * @return the list of entities
     */
    
    @Transactional(readOnly = true)
    public List<TagDTO> findAll() {
        log.debug("Request to get all Tags");
        
        Map<Long, Long> tagsByPopularity = tagsByPopularity();
        
        return tagRepository.findAll().stream()
        	.map(tagMapper::toDto)
            .map(dto ->  {
            	dto.setPopularity(tagsByPopularity.get(dto.getId()));
            	return dto;
            })
            .collect(Collectors.toList());
    }

	
	public List<TagDTO> findAllPublic() {
		Map<Long, Long> tagsByPopularity = tagsByPopularity();
		
		return tagRepository.findByVisible(true).stream()
            .map(tagMapper::toDto)
            .map(dto ->  {
            	dto.setPopularity(tagsByPopularity.get(dto.getId()));
            	return dto;
            })
            .collect(Collectors.toList());
	}

    /**
     * Get one tag by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    
    @Transactional(readOnly = true)
    public Optional<TagDTO> findOne(Long id) {
        log.debug("Request to get Tag : {}", id);
        return tagRepository.findById(id)
            .map(tagMapper::toDto);
    }

    /**
     * Delete the tag by id.
     *
     * @param id the id of the entity
     */
    
    public void delete(Long id) {
        log.debug("Request to delete Tag : {}", id);
        tagRepository.deleteById(id);
    }

	
	public List<ProblemDTO> problemsForTag(Long tagId) {
		Tag tag = tagRepository.getOne(tagId);
		return tagCollectionTagRepository
			.findByTag(tag)
			.stream()
			.map(tgt -> tgt.getCollection().getId())
			.map(collectionId -> problemRepository.findByTagsId(collectionId))
			.filter(optional -> optional.isPresent())
			.map(optional -> optional.get())
			.map(problemMapper::toDto)
			.collect(Collectors.toList());
	}

	
	public Tag findOrCreateNew(TagDTO dto) {
		if (dto.getId() == null) {
			Optional<Tag> res = tagRepository.findOneByTitle(dto.getTitle().trim());
			if (res.isPresent()) {
				return res.get();
			} else {
				Tag tag = tagMapper.toEntity(dto);
				tag = tagRepository.save(tag);
				return tag;
			}
		} else {
			return tagRepository.findById(dto.getId()).get();
		}
	}	
	
	
	public TagCollection updateTagsForCollection(TagCollection collection, List<TagDTO> newTags) {
		if (collection == null) {
			collection = new TagCollection();
    		collection = tagCollectionRepository.save(collection);
    	} else {
	    	tagCollectionTagRepository.deleteByCollection(collection);
    	}
    	
		final TagCollection finalCollection = collection; // needed by map()
		
    	List<TagCollectionTag> newTCT = newTags.stream()
    		.map(dto -> findOrCreateNew(dto))
    		.map(tag -> new TagCollectionTag()
    				.collection(finalCollection)
    				.tag(tag))
    		.collect(Collectors.toList());
    	
    	tagCollectionTagRepository.saveAll(newTCT);
    	return collection;
	}

	
	public Stream<Tag> findTagsForCollection(TagCollection collection) {
    	if (collection == null) {
    		return new ArrayList<Tag>().stream();
    	} else {
	    	return tagCollectionTagRepository
	    		.findByCollection(collection)
	    		.stream()
	    		.map(tgt -> tgt.getTag());
    	}
	}

	private Stream<Submission> submissionsForTagImpl(Long id) {
		Tag tag = tagRepository.getOne(id);
		return tagCollectionTagRepository
			.findByTag(tag)
			.stream()
			.map(tgt -> tgt.getCollection().getId())
			.map(collectionId -> submissionRepository.findByTagsId(collectionId))
			.filter(optional -> optional.isPresent())
			.map(optional -> optional.get());
	}
	
	
	public List<SubmissionDTO> submissionsForTag(Long id) {
		return submissionsForTagImpl(id)
			.map(submissionMapper::toDto)
			.collect(Collectors.toList());
	}

	
	public Stream<CompetitionProblem> problemsTaggedByUsers(Long id) {
		return submissionsForTagImpl(id)
			.map(submission -> submission.getCompetitionProblem())
			.distinct();
	}

	
	public Map<Long, Long> tagsByPopularity() {
		List<Object[]> tagsByPopularity = tagCollectionTagRepository.tagsByPopularity();
		Map<Long, Long> res = new HashMap<>();
		for (Object[] o : tagsByPopularity) {
			Long tagId = (Long) o[0];
			Long popularity = (Long) o[1]; 
			res.put(tagId, popularity);
		}
		return res;
	}

}
