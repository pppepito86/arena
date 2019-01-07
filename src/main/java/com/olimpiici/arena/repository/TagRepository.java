package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.Tag;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the Tag entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
	Optional<Tag> findOneByTitle(String title);
	List<Tag> findByVisible(Boolean visible);
}
