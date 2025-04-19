package com.olimpiici.arena.repository;


import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;


/**
 * Spring Data  repository for the CompetitionProblem entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CompetitionProblemRepository extends JpaRepository<CompetitionProblem, Long> {
	Page<CompetitionProblem> findByCompetition(Competition parent, Pageable pageable);
	
	List<CompetitionProblem> findByCompetition(Competition parent);
	
	List<CompetitionProblem> findByCompetitionIn(Collection<Competition> parent);
	
	List<CompetitionProblem> findByProblemId(Long problemId);
}
