package com.olimpiici.arena.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.dto.Stats.Queue;
import com.olimpiici.arena.service.dto.Stats.QueuedSubmission;

public class StatsResourceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private UserRepository userRepository;

    private StatsResource statsResource;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        statsResource = new StatsResource(submissionRepository, problemRepository, userRepository);
    }

    @Test
    public void testGetQueue_OrdersJudgingFirstThenOldestWaitingAndMapsDetails() {
        // findQueue() returns newest-first (ORDER BY id DESC)
        Submission newestWaiting = buildSubmission(103L, "waiting", "Ivan", "Petrov", "ivanp", "Triangle", ZonedDateTime.now().minusMinutes(1));
        Submission judging = buildSubmission(102L, "judging", null, null, "coder42", "Graphs", ZonedDateTime.now().minusMinutes(3));
        Submission oldestWaiting = buildSubmission(101L, "waiting", "Maria", null, "maria", "DP Task", ZonedDateTime.now().minusMinutes(5));

        when(submissionRepository.findQueue()).thenReturn(Arrays.asList(newestWaiting, judging, oldestWaiting));

        Queue queue = statsResource.getQueue();

        assertThat(queue.submissionIds).containsExactly(103L, 102L, 101L);
        assertThat(queue.ageMins).isGreaterThanOrEqualTo(5);
        assertThat(queue.submissions).hasSize(3);

        QueuedSubmission first = queue.submissions.get(0);
        assertThat(first.id).isEqualTo(102L);
        assertThat(first.verdict).isEqualTo("judging");
        assertThat(first.author).isEqualTo("coder42");
        assertThat(first.problemName).isEqualTo("Graphs");

        QueuedSubmission second = queue.submissions.get(1);
        assertThat(second.id).isEqualTo(101L);
        assertThat(second.verdict).isEqualTo("waiting");
        assertThat(second.author).isEqualTo("Maria");
        assertThat(second.problemName).isEqualTo("DP Task");

        QueuedSubmission third = queue.submissions.get(2);
        assertThat(third.id).isEqualTo(103L);
        assertThat(third.verdict).isEqualTo("waiting");
        assertThat(third.author).isEqualTo("Ivan Petrov");
        assertThat(third.problemName).isEqualTo("Triangle");
    }

    @Test
    public void testGetQueue_HandlesMissingAssociationsAndEmptyQueue() {
        when(submissionRepository.findQueue()).thenReturn(Collections.emptyList());

        Queue emptyQueue = statsResource.getQueue();
        assertThat(emptyQueue.submissionIds).isEmpty();
        assertThat(emptyQueue.submissions).isEmpty();
        assertThat(emptyQueue.ageMins).isEqualTo(0);

        Submission orphan = new Submission();
        orphan.setId(200L);
        orphan.setVerdict("waiting");
        when(submissionRepository.findQueue()).thenReturn(Collections.singletonList(orphan));

        Queue queue = statsResource.getQueue();
        assertThat(queue.submissions).hasSize(1);
        assertThat(queue.submissions.get(0).id).isEqualTo(200L);
        assertThat(queue.submissions.get(0).author).isNull();
        assertThat(queue.submissions.get(0).problemName).isNull();
        assertThat(queue.ageMins).isEqualTo(-1);

        Submission timed = buildSubmission(201L, "waiting", "Ana", null, "ana", "Sort", ZonedDateTime.now().minusMinutes(7));
        when(submissionRepository.findQueue()).thenReturn(Arrays.asList(timed, orphan));

        Queue mixedQueue = statsResource.getQueue();
        assertThat(mixedQueue.ageMins).isGreaterThanOrEqualTo(7);
    }

    private Submission buildSubmission(
        Long id,
        String verdict,
        String firstName,
        String lastName,
        String login,
        String problemTitle,
        ZonedDateTime uploadDate
    ) {
        User user = new User();
        user.setLogin(login);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        Problem problem = new Problem();
        problem.setTitle(problemTitle);

        CompetitionProblem cp = new CompetitionProblem();
        cp.setProblem(problem);

        Submission s = new Submission();
        s.setId(id);
        s.setVerdict(verdict);
        s.setUser(user);
        s.setCompetitionProblem(cp);
        s.setUploadDate(uploadDate);
        return s;
    }
}
