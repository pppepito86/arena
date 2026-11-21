package com.olimpiici.arena.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.olimpiici.arena.domain.Authority;
import com.olimpiici.arena.domain.Comment;
import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.Topic;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.security.AuthoritiesConstants;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryIntTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private CompetitionProblemRepository competitionProblemRepository;

    @Autowired
    private EntityManager em;

    private CompetitionProblem cp;
    private Authority adminAuthority;

    @Before
    public void init() {
        em.createQuery("DELETE FROM Comment").executeUpdate();
        em.createQuery("DELETE FROM TopicSubscriptions").executeUpdate();
        em.createQuery("DELETE FROM Topic").executeUpdate();
        submissionRepository.deleteAll();
        competitionProblemRepository.deleteAll();
        problemRepository.deleteAll();
        competitionRepository.deleteAll();
        userRepository.deleteAll();
        em.flush();

        adminAuthority = authorityRepository.findById(AuthoritiesConstants.ADMIN).orElseGet(() -> {
            Authority a = new Authority();
            a.setName(AuthoritiesConstants.ADMIN);
            return authorityRepository.saveAndFlush(a);
        });

        Competition comp = new Competition();
        comp.setLabel("Comp");
        competitionRepository.saveAndFlush(comp);

        Problem problem = new Problem();
        problem.setTitle("Prob");
        problemRepository.saveAndFlush(problem);

        cp = new CompetitionProblem();
        cp.setCompetition(comp);
        cp.setProblem(problem);
        competitionProblemRepository.saveAndFlush(cp);
    }

    private User insertUser(
            String login,
            boolean activated,
            String activationKey,
            Instant createdDate,
            Instant lastModifiedDate,
            String lastModifiedBy,
            boolean isAdmin) {
        User user = new User();
        user.setLogin(login);
        user.setPassword("passwordpasswordpasswordpasswordpasswordpasswordpasswordpass");
        user.setEmail(login + "@example.com");
        user.setActivated(activated);
        user.setActivationKey(activationKey);
        user.setCreatedBy("anonymousUser");
        if (isAdmin) {
            user.setAuthorities(Collections.singleton(adminAuthority));
        }
        user = userRepository.saveAndFlush(user);

        // Auditing overwrites created/lastModified metadata on insert; set exact values via native SQL.
        em.createNativeQuery(
                "UPDATE jhi_user SET created_date = ?, last_modified_date = ?, last_modified_by = ? WHERE id = ?")
            .setParameter(1, java.sql.Timestamp.from(createdDate))
            .setParameter(2, lastModifiedDate != null ? java.sql.Timestamp.from(lastModifiedDate) : null)
            .setParameter(3, lastModifiedBy)
            .setParameter(4, user.getId())
            .executeUpdate();
        em.clear();
        return userRepository.findById(user.getId()).get();
    }

    @Test
    public void testFindAllRemovableNotActivatedUsers_ExcludesReferencedDeactivatedEmailChangedAndAdminUsers() {
        Instant now = Instant.now();
        Instant old = now.minus(5, ChronoUnit.DAYS);
        Instant recent = now.minus(1, ChronoUnit.DAYS);
        Instant cutoff = now.minus(3, ChronoUnit.DAYS);

        User removable = insertUser("removable", false, "key1", old, old, "anonymousUser", false);
        User recentSignup = insertUser("recent_signup", false, "key2", recent, recent, "anonymousUser", false);
        User adminDeactivated = insertUser("admin_deactivated", false, null, old, old, "admin", false);
        User selfEmailChanged = insertUser("self_changed_email", false, "key3", old, old, "self_changed_email", false);
        User unactivatedAdmin = insertUser("unactivated_admin", false, "key4", old, old, "system", true);
        User withSubmission = insertUser("with_submission", false, "key5", old, old, "anonymousUser", false);
        User withTopic = insertUser("with_topic", false, "key6", old, old, "anonymousUser", false);
        User withCommentAuthor = insertUser("with_comment_author", false, "key7", old, old, "anonymousUser", false);
        User withCommentCensurer = insertUser("with_comment_censurer", false, "key8", old, old, "anonymousUser", false);
        User withTopicSubscription = insertUser("with_topic_sub", false, "key9", old, old, "anonymousUser", false);
        User activated = insertUser("activated", true, null, old, old, "anonymousUser", false);

        Submission sub = new Submission();
        sub.setUser(withSubmission);
        sub.setCompetitionProblem(cp);
        sub.setPoints(100);
        sub.setUploadDate(ZonedDateTime.now());
        submissionRepository.saveAndFlush(sub);

        Topic topic = new Topic();
        topic.setTitle("Topic");
        topic.setCreator(withTopic);
        topic.setCreatedDate(ZonedDateTime.now());
        em.persist(topic);
        em.flush();

        Comment comment = new Comment();
        comment.setTopicId(topic.getId());
        comment.setAuthor(withCommentAuthor);
        comment.setContent("Hello");
        comment.setPostedDate(ZonedDateTime.now());
        em.persist(comment);
        em.flush();

        em.createNativeQuery("UPDATE comment SET censured_by = ? WHERE id = ?")
            .setParameter(1, withCommentCensurer.getId())
            .setParameter(2, comment.getId())
            .executeUpdate();

        em.createNativeQuery("INSERT INTO topic_subscriptions (user_id, topic_id) VALUES (?, ?)")
            .setParameter(1, withTopicSubscription.getId())
            .setParameter(2, topic.getId())
            .executeUpdate();
        em.flush();

        List<User> candidates = userRepository.findAllRemovableNotActivatedUsers(cutoff);
        assertThat(candidates).extracting(User::getLogin).containsExactly("removable");

        assertThat(userRepository.isRemovableNotActivatedUser(removable.getId())).isTrue();
        assertThat(userRepository.isRemovableNotActivatedUser(recentSignup.getId())).isTrue();
        assertThat(userRepository.isRemovableNotActivatedUser(adminDeactivated.getId())).isFalse();
        assertThat(userRepository.isRemovableNotActivatedUser(selfEmailChanged.getId())).isFalse();
        assertThat(userRepository.isRemovableNotActivatedUser(unactivatedAdmin.getId())).isFalse();
        assertThat(userRepository.isRemovableNotActivatedUser(withSubmission.getId())).isFalse();
        assertThat(userRepository.isRemovableNotActivatedUser(withTopic.getId())).isFalse();
        assertThat(userRepository.isRemovableNotActivatedUser(withCommentAuthor.getId())).isFalse();
        assertThat(userRepository.isRemovableNotActivatedUser(withCommentCensurer.getId())).isFalse();
        assertThat(userRepository.isRemovableNotActivatedUser(withTopicSubscription.getId())).isFalse();
        assertThat(userRepository.isRemovableNotActivatedUser(activated.getId())).isFalse();

        // Deleting returned candidates must succeed without FK errors
        userRepository.deleteAll(candidates);
        userRepository.flush();
        assertThat(userRepository.findById(removable.getId())).isEmpty();
    }
}
