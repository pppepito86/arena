package com.olimpiici.arena.service;

import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Tag;
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.dto.TagDTO;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
    List<TagDTO> findAllPublic();

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
    
    Tag findOrCreateNew(TagDTO dto);
    
    List<ProblemDTO> problemsForTag(Long tagId);
    
    Stream<Tag> findTagsForCollection(TagCollection collection);
    
    // Removes all old tags for a collection and replaces them with the 
    // new tags.
    // If "collection" is null a new creation is created and then returned;
    // Tags are identified by their ID. Id a tag doesn't 
    // have and ID, a tag with same title is used or if such doesn't 
    // exist a new one is created.
    // Return value: the collection to which the elements were added - 
    // either "collection" or a new collection.
    TagCollection updateTagsForCollection(TagCollection collection, 
    		List<TagDTO> newTags);

	List<SubmissionDTO> submissionsForTag(Long id);
	
	Stream<CompetitionProblem> problemsTaggedByUsers(Long id);
	
	Map<Long, Long> tagsByPopularity();
}
