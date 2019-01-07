package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.Problem;

import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the Problem entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
	Optional<Problem> findByTagsId(Long tagsId);
}
