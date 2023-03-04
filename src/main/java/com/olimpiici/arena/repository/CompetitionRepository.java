package com.olimpiici.arena.repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.olimpiici.arena.domain.Competition;


/**
 * Spring Data  repository for the Competition entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
	Page<Competition> findByParent(Competition parent, Pageable pageable);

	List<Competition> findByParent(Competition parent);

	List<Competition> findByParentIn(Collection<Competition> parent);

	@Query(
		value =
			" select user_points_all.user_id, "
			+ "     jhi_user.first_name,"
			+ "     jhi_user.last_name,  "
			+ "     sum(user_points_all.max_points) - coalesce(sum(user_points_old.max_points),0) as total_points"
			+ " from"
			+ "     ("
			+ "         select user_id, competition_problem_id, max(points) as max_points"
			+ "         from submission"
			+ "         where upload_date < ?"
			+ "         group by user_id, competition_problem_id"
			+ "     ) as user_points_old"
			+ "     right join"
			+ "     ("
			+ "         select user_id, competition_problem_id, max(points) as max_points"
			+ "         from submission"
			+ "         group by user_id, competition_problem_id"
			+ "     ) as user_points_all"
			+ "     on user_points_old.user_id = user_points_all.user_id "
			+ "     and user_points_old.competition_problem_id = user_points_all.competition_problem_id"
			+ " join jhi_user"
			+ " on jhi_user.id = user_points_all.user_id"
			+ " where user_points_all.user_id != 4"  // Not the author
			+ " and user_points_all.user_id != 2032"  // Not Pesho Orgov
			+ " group by user_id"
			+ " order by total_points desc"
			+ " limit ?, ?;",
		nativeQuery = true
	)
	List<Object[]> getRootStandings(ZonedDateTime timeSince, long pageOffset, long pageSize);

	@Query(
		value =
			"select count(distinct user_id)"
			+ " from submission"
			+ " where user_id != 4 " // Not the author
			+ " and user_id != 2032"  // Not Pesho Orgov
			+ " and upload_date > ?1 ;",
		nativeQuery = true
	)
	Integer getRootStandingsSize(ZonedDateTime timeSince);

	String tableWithStandingsPerProblem =
			  "     ("
			+ "         select user_id, competition_problem_id, max(points) as max_points"
			+ "         from submission"
			+ "         where upload_date < ?1"
			+ "         group by user_id, competition_problem_id"
			+ "     ) as user_points_old"
			+ "     right join"
			+ "     ("
			+ "         select user_id, competition_problem_id, max(points) as max_points"
			+ "         from submission"
			+ "         group by user_id, competition_problem_id"
			+ "     ) as user_points_all"
			+ " 	on user_points_old.user_id = user_points_all.user_id "
			+ "     	and user_points_old.competition_problem_id = user_points_all.competition_problem_id"
			+ " join jhi_user"
			+ " on jhi_user.id = user_points_all.user_id"
			+ " where user_points_all.competition_problem_id in ?2"
			+ " 	and user_points_all.user_id != 4"  // Not the author
			+ " 	and user_points_all.user_id != 2032"  // Not Pesho Orgov
			+ " group by user_id";

	String aggregatedStandingsCommonQuery =
			  " select user_points_all.user_id, "
			+ "     jhi_user.first_name,"
			+ "     jhi_user.last_name,  "
			+ "     sum(user_points_all.max_points) - coalesce(sum(user_points_old.max_points),0) as total_points"
			+ " from" + tableWithStandingsPerProblem;


	@Query(
		value = aggregatedStandingsCommonQuery
			+ " order by total_points desc"
			+ " limit ?3 , ?4 ;",
		nativeQuery = true
	)
	List<Object[]> getAggregatedStandingsForProblems(ZonedDateTime timeSince, List<Long> problemIds, long pageOffset, long pageSize);

	@Query(
		value =
			"select count(distinct user_id)"
			+ " from submission"
			+ " where user_id != 4 " // Not the author
			+ " and user_id != 2032"  // Not Pesho Orgov
			+ " and upload_date > ?1"
			+ " and competition_problem_id in ?2 ;",
		nativeQuery = true
	)
	Integer getAggregatedStandingsSizeForProblems(ZonedDateTime timeSince, List<Long> problemIds);

	@Query(
		value = aggregatedStandingsCommonQuery
			+ " having user_points_all.user_id = ?3 ;",
		nativeQuery = true
	)
	List<Object[]> getAggregatedUserPointsForProblems(ZonedDateTime timeSince, List<Long> problemIds, Long userId);

	String standingsPerProblemCommonQuery =
			  " select user_points_all.user_id, "
		    + "     jhi_user.first_name,"
			+ "     jhi_user.last_name,  "
			+ "     sum(user_points_all.max_points) - coalesce(sum(user_points_old.max_points),0) as total_points,"
			+ "     CONVERT("
			+ "			JSON_OBJECTAGG("
			+ "				user_points_all.competition_problem_id, "
		    + "				user_points_all.max_points - coalesce(user_points_old.max_points,0)"
			+ "			), "
		    + "         CHAR"
		    + " 	) as problems "
			+ " from" + tableWithStandingsPerProblem;

	@Query(
			value = standingsPerProblemCommonQuery
				+ " order by total_points desc"
				+ " limit ?3 , ?4 ;",
			nativeQuery = true
		)
	List<Object[]> getStandingsPerProblemForProblems(ZonedDateTime timeSince, List<Long> problemIds, long pageOffset, long pageSize);

	@Query(
		value = standingsPerProblemCommonQuery
			+ " having user_points_all.user_id = ?3 ;",
		nativeQuery = true
	)
	List<Object[]> getUserPointsPerProblem(ZonedDateTime timeSince, List<Long> problemIds, Long userId);

	@Query(
			value = "select competition_problem_id, max(points)"
					+ " from submission"
					+ " where user_id = ?2 "
					+ " and competition_problem_id in ?1"
					+ " group by 1;",
			nativeQuery = true
		)
	List<Object[]> getSimpleUserPointsPerProblem(List<Long> compProblemIds, Long userId);
}
