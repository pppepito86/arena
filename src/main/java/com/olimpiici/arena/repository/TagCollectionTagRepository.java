package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.Tag;
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.domain.TagCollectionTag;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the TagCollectionTag entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TagCollectionTagRepository extends JpaRepository<TagCollectionTag, Long> {
	List<TagCollectionTag> findByCollection(TagCollection collection);
	List<TagCollectionTag> findByTag(Tag tag);
	void deleteByCollection(TagCollection collection);
	
	@Query("SELECT t.tag.id, COUNT(t.collection) FROM TagCollectionTag t GROUP BY t.tag.id")
	List<Object[]> tagsByPopularity();
}
