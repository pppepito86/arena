package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data  repository for the Submission entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Query("select submission from Submission submission where submission.user.login = ?#{principal.username}")
    List<Submission> findByUserIsCurrentUser();
    
    List<Submission> findByUser(User user);
    
    Page<Submission> findByUser(User user, Pageable pageable);
    
    Page<Submission> findByCompetitionProblem(
    		CompetitionProblem competitionProblem, Pageable pageable);
    
    List<Submission> findByCompetitionProblemAndUser(
    		CompetitionProblem competitionProblem, User user);
    
    Page<Submission> findByCompetitionProblemAndUser(
    		CompetitionProblem competitionProblem, User user, Pageable pageable);
    
    Page<Submission> findByUserAndCompetitionProblemIn(
    		User user, Collection<CompetitionProblem> competitionProblem, Pageable pageable);
    
    List<Submission> findByUserAndCompetitionProblemIn(
    		User user, Collection<CompetitionProblem> competitionProblem);
    
    Page<Submission> findByCompetitionProblemIn(
    		Collection<CompetitionProblem> competitionProblem, Pageable pageable);
    
    List<Submission> findByCompetitionProblemIn(
    		Collection<CompetitionProblem> competitionProblem);
    
    List<Submission> findByVerdict(String verdict);
    
    Optional<Submission> findByTagsId(Long tagsId);
}
