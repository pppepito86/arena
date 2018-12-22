package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Submission;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data  repository for the Submission entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Query("select submission from Submission submission where submission.user.login = ?#{principal.username}")
    List<Submission> findByUserIsCurrentUser();

}
