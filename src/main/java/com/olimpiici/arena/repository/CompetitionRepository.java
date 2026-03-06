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
 * Spring Data repository for the Competition entity.
 * This interface handles database operations for competitions, including custom 
 * complex queries for calculating user standings and rankings.
 */
@SuppressWarnings("unused")
@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
	
	/**
	 * Finds sub-competitions belonging to a specific parent competition, with support for pagination.
	 */
	Page<Competition> findByParent(Competition parent, Pageable pageable);

	/**
	 * Finds all sub-competitions belonging to a specific parent competition.
	 */
	List<Competition> findByParent(Competition parent);

	/**
	 * Finds all sub-competitions that belong to any of the provided parent competitions.
	 */
	List<Competition> findByParentIn(Collection<Competition> parent);

	/**
	 * Calculates the overall user standings (rankings) across the entire platform.
	 * It calculates the total points for each user by taking their current maximum 
	 * score per problem and subtracting the maximum score they had before 'timeSince'.
	 * This effectively shows who has been most active/successful in the recent period.
	 *
	 * @param timeSince The starting point in time for calculating point deltas.
	 * @param pageOffset Pagination offset.
	 * @param pageSize Number of records per page.
	 * @return A list of objects containing user ID, names, and total points gained.
	 */
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
			+ " group by user_points_all.user_id"
			+ " order by total_points desc"
			+ " limit ?, ?;",
		nativeQuery = true
	)
	List<Object[]> getRootStandings(ZonedDateTime timeSince, long pageOffset, long pageSize);

	/**
	 * Gets the total number of unique users who have made at least one submission 
	 * since the specified time. This is used for calculating the number of pages 
	 * in the global standings.
	 */
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

	/**
	 * Internal query snippet that calculates point deltas for a specific set of problems.
	 */
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
			+ " 	and user_points_all.user_id != 2032"; // Not Pesho Orgov

	/**
	 * Base query for calculating aggregated standings (total points) for a specific set of problems.
	 */
	String aggregatedStandingsCommonQuery =
			  " select user_points_all.user_id, "
			+ "     jhi_user.first_name,"
			+ "     jhi_user.last_name,  "
			+ "     sum(user_points_all.max_points) - coalesce(sum(user_points_old.max_points),0) as total_points"
			+ " from" + tableWithStandingsPerProblem
			+ " group by user_points_all.user_id";


	/**
	 * Calculates user standings for a specific subset of problems.
	 *
	 * @param timeSince Point in time to calculate deltas from.
	 * @param problemIds List of CompetitionProblem IDs to include.
	 * @param pageOffset Pagination offset.
	 * @param pageSize Number of records per page.
	 * @return List of ranking data (user info and total points).
	 */
	@Query(
		value = aggregatedStandingsCommonQuery
			+ " order by total_points desc"
			+ " limit ?3 , ?4 ;",
		nativeQuery = true
	)
	List<Object[]> getAggregatedStandingsForProblems(ZonedDateTime timeSince, List<Long> problemIds, long pageOffset, long pageSize);

	/**
	 * Gets the total number of unique users who submitted solutions to the 
	 * specified problems since the given time.
	 */
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

	/**
	 * Calculates the total points gained by a specific user for a specific set of problems.
	 */
	@Query(
		value = aggregatedStandingsCommonQuery
			+ " having user_points_all.user_id = ?3 ;",
		nativeQuery = true
	)
	List<Object[]> getAggregatedUserPointsForProblems(ZonedDateTime timeSince, List<Long> problemIds, Long userId);

	/**
	 * Base query for calculating standings per problem, including a JSON breakdown of scores.
	 */
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
		    + "         VARCHAR"
		    + " 	) as problems "
			+ " from" + tableWithStandingsPerProblem
			+ " group by user_points_all.user_id";

	/**
	 * Calculates detailed standings for a set of problems, including a breakdown 
	 * of points for each individual problem formatted as a JSON string.
	 *
	 * @param timeSince Point in time to calculate deltas from.
	 * @param problemIds List of CompetitionProblem IDs to include.
	 * @param pageOffset Pagination offset.
	 * @param pageSize Number of records per page.
	 * @return Ranking data with per-problem point breakdown.
	 */
	@Query(
			value = standingsPerProblemCommonQuery
				+ " order by total_points desc"
				+ " limit ?3 , ?4 ;",
			nativeQuery = true
		)
	List<Object[]> getStandingsPerProblemForProblems(ZonedDateTime timeSince, List<Long> problemIds, long pageOffset, long pageSize);

	/**
	 * Gets the detailed point breakdown (including per-problem scores) for a 
	 * single specific user across a set of problems.
	 */
	@Query(
		value = standingsPerProblemCommonQuery
			+ " having user_points_all.user_id = ?3 ;",
		nativeQuery = true
	)
	List<Object[]> getUserPointsPerProblem(ZonedDateTime timeSince, List<Long> problemIds, Long userId);

	/**
	 * Retrieves the maximum points achieved by a specific user for each problem 
	 * in a provided list. This is a simpler version that doesn't calculate 
	 * time-based deltas.
	 *
	 * @param compProblemIds List of competition problems to check.
	 * @param userId ID of the user.
	 * @return List of pairs [competition_problem_id, max_points].
	 */
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
