package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Competition;

import java.util.List;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the Competition entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
	List<Competition> findByParent(Long parent);
}
