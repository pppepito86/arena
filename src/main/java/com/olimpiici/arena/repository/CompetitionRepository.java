package com.olimpiici.arena.repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import com.olimpiici.arena.domain.Competition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

	@Modifying
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
			+ " where user_points_all.user_id != 4" 
			+ " group by user_id"
			+ " order by total_points desc"
			+ " limit ?, ?;",
		countQuery = "select count(distinct user_id) from submission;",
		nativeQuery = true
	)
	List<Object[]> getRootStandings(ZonedDateTime timeSince, long pageOffset, long pageSize);
}
