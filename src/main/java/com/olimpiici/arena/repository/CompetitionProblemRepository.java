package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.CompetitionProblem;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the CompetitionProblem entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CompetitionProblemRepository extends JpaRepository<CompetitionProblem, Long> {

}
