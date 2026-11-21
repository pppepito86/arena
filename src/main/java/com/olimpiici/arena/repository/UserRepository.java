package com.olimpiici.arena.repository;

import com.olimpiici.arena.domain.User;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

/**
 * Spring Data JPA repository for the User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    String USERS_BY_LOGIN_CACHE = "usersByLogin";

    String USERS_BY_EMAIL_CACHE = "usersByEmail";

    Optional<User> findOneByActivationKey(String activationKey);

    /**
     * Finds abandoned, never-activated signups that are safe to delete.
     * Excludes admin-deactivated accounts (whose activationKey was cleared to null upon
     * initial activation), existing users who changed their own email (lastModifiedBy == login
     * or modified recently), admins, and any user referenced by foreign keys.
     */
    @Query(
        "SELECT u FROM User u " +
        "WHERE u.activated = false " +
        "AND u.activationKey IS NOT NULL " +
        "AND u.createdDate < :dateTime " +
        "AND (u.lastModifiedDate IS NULL OR u.lastModifiedDate < :dateTime) " +
        "AND (u.lastModifiedBy IS NULL OR u.lastModifiedBy <> u.login) " +
        "AND NOT EXISTS (SELECT 1 FROM User u2 JOIN u2.authorities a WHERE u2 = u AND a.name = 'ROLE_ADMIN') " +
        "AND NOT EXISTS (SELECT 1 FROM Submission s WHERE s.user = u) " +
        "AND NOT EXISTS (SELECT 1 FROM Topic t WHERE t.creator = u) " +
        "AND NOT EXISTS (SELECT 1 FROM Comment c WHERE c.author = u OR c.censuredBy = u) " +
        "AND NOT EXISTS (SELECT 1 FROM TopicSubscriptions ts WHERE ts.user = u)"
    )
    List<User> findAllRemovableNotActivatedUsers(@Param("dateTime") Instant dateTime);

    /**
     * Checks whether a specific unactivated user is a never-activated signup safe to
     * replace on re-registration (same protections as {@link #findAllRemovableNotActivatedUsers},
     * without the age cutoff).
     */
    @Query(
        "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
        "WHERE u.id = :userId " +
        "AND u.activated = false " +
        "AND u.activationKey IS NOT NULL " +
        "AND (u.lastModifiedBy IS NULL OR u.lastModifiedBy <> u.login) " +
        "AND NOT EXISTS (SELECT 1 FROM User u2 JOIN u2.authorities a WHERE u2 = u AND a.name = 'ROLE_ADMIN') " +
        "AND NOT EXISTS (SELECT 1 FROM Submission s WHERE s.user = u) " +
        "AND NOT EXISTS (SELECT 1 FROM Topic t WHERE t.creator = u) " +
        "AND NOT EXISTS (SELECT 1 FROM Comment c WHERE c.author = u OR c.censuredBy = u) " +
        "AND NOT EXISTS (SELECT 1 FROM TopicSubscriptions ts WHERE ts.user = u)"
    )
    boolean isRemovableNotActivatedUser(@Param("userId") Long userId);

    Optional<User> findOneByResetKey(String resetKey);

    Optional<User> findOneByEmailIgnoreCase(String email);

    Optional<User> findOneByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesById(Long id);

    @EntityGraph(attributePaths = "authorities")
    @Cacheable(cacheNames = USERS_BY_LOGIN_CACHE)
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    @Cacheable(cacheNames = USERS_BY_EMAIL_CACHE)
    Optional<User> findOneWithAuthoritiesByEmail(String email);

    Page<User> findAllByLoginNot(Pageable pageable, String login);
}
