package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.TagCollectionTag;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the TagCollectionTag entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TagCollectionTagRepository extends JpaRepository<TagCollectionTag, Long> {

}
