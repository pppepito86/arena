package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.Tag;
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.domain.TagCollectionTag;

import java.util.List;

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
}
