package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.Competition;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the Competition entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
	Page<Competition> findByParent(Competition parent, Pageable pageable);
	
	List<Competition> findByParent(Competition parent);
	
	List<Competition> findByParentIn(Collection<Competition> parent);
}
