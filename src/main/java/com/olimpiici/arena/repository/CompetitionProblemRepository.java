package com.olimpiici.arena.repository;


import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the CompetitionProblem entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CompetitionProblemRepository extends JpaRepository<CompetitionProblem, Long> {
	Page<CompetitionProblem> findByCompetition(Competition parent, Pageable pageable);
	
	List<CompetitionProblem> findByCompetition(Competition parent);
	
	List<CompetitionProblem> findByCompetitionIn(Collection<Competition> parent);
	
	Optional<CompetitionProblem> findOneByProblemId(Long problemId);
	
	List<CompetitionProblem> findByProblemId(Long problemId);
}
