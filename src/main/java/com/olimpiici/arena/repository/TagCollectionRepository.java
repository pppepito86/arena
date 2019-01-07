package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.TagCollection;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the TagCollection entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TagCollectionRepository extends JpaRepository<TagCollection, Long> {

}
