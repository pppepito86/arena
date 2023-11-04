package com.olimpiici.arena.repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;


/**
 * Spring Data  repository for the Submission entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Query("select submission from Submission submission where submission.user.login = ?#{principal.username}")
    List<Submission> findByUserIsCurrentUser();

    @Query("select submission from Submission submission"
        + " where submission.verdict='waiting' or submission.verdict='judging'"
        + " order by submission.id desc")
    List<Submission> findQueue();

    @Query("select submission from Submission submission"
        + " where submission.verdict='' and submission.user.id != 4") // Empty verdict and not author solution
    List<Submission> findBadSubmissions();

    @Query("select submission.uploadDate from Submission submission")
    List<ZonedDateTime> findAllUploadDates();

    @Query(
        value =
            " select * from submission" +
            " where upload_date " + 
            "   between DATE_SUB(NOW(), INTERVAL ?1 DAY) and DATE_SUB(NOW(), INTERVAL ?2 DAY)",
        nativeQuery = true
    )
    List<Submission> findSubmissionsInPeriod(Integer fromDaysBack, Integer toDaysBack);

    @Query(
		value =
    		" select count(distinct user_id)" +
    		" from submission" +
    		" where upload_date is not null" +
    		" and upload_date >= ?" +
    		" and upload_date < ?",
		nativeQuery = true
	)
    Integer findNumSubmitters(ZonedDateTime from, ZonedDateTime to);

    @Query(
        value =
            " select upload_date " +
            " from submission" +
            " where user_id = ?" +
            " order by id desc " +
            " limit 1",
        nativeQuery = true
    )
    Optional<java.sql.Timestamp> findLastByUser(Long userId);

    @Query(
        value =
            " select count(*) " +
            " from submission" +
            " where user_id = ?" +
            " and upload_date > DATE_SUB(NOW(), INTERVAL 1 DAY)",
        nativeQuery = true
    )
    Integer numSubmissionsLastDay(Long userId);

    List<Submission> findByUser(User user);

    Page<Submission> findByUser(User user, Pageable pageable);

    List<Submission> findAllByCompetitionProblem(
    		CompetitionProblem competitionProblem);

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

    Stream<Submission> findByCompetitionProblemIn(
    		Collection<CompetitionProblem> competitionProblem);

    List<Submission> findByVerdict(String verdict);

    Optional<Submission> findByTagsId(Long tagsId);
}
