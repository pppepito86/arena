package com.olimpiici.arena.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class CompetitionRepositoryIntTest {

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private CompetitionProblemRepository competitionProblemRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private EntityManager em;

    private User user1;
    private User user2;
    private User user3;
    
    private CompetitionProblem cp1;
    private CompetitionProblem cp2;
    private ZonedDateTime now;
    private ZonedDateTime past;

    public static String jsonObjectAgg(String key, String value) {
        return String.format("{\"%s\":%s}", key, value);
    }
    
    @Before
    public void init() {
        // Clean up in reverse order of dependencies
        submissionRepository.deleteAll();
        competitionProblemRepository.deleteAll();
        problemRepository.deleteAll();
        competitionRepository.deleteAll();
        userRepository.deleteAll();

        em.createNativeQuery("CREATE ALIAS IF NOT EXISTS JSON_OBJECTAGG FOR \"com.olimpiici.arena.repository.CompetitionRepositoryIntTest.jsonObjectAgg\"").executeUpdate();
        
        now = ZonedDateTime.now();
        past = now.minusDays(1);
        ZonedDateTime wayPast = past.minusDays(1);
        ZonedDateTime afterPast = past.plusHours(1);

        // Native inserts to control IDs for exclusion testing
        insertUser(1L, "user1", "Fn1", "Ln1");
        insertUser(2L, "user2", "Fn2", "Ln2");
        insertUser(3L, "user3", "Fn3", "Ln3");
        insertUser(4L, "author_excluded", "Author", "Excluded");
        insertUser(2032L, "pesho_excluded", "Pesho", "Excluded");
        insertUser(6L, "user6_paged", "Fn6", "Ln6");

        user1 = userRepository.findById(1L).get();
        user2 = userRepository.findById(2L).get();
        user3 = userRepository.findById(3L).get();

        Competition competition = new Competition();
        competition.setLabel("Comp1");
        competitionRepository.saveAndFlush(competition);

        Problem p1 = new Problem();
        p1.setTitle("Problem 1");
        problemRepository.saveAndFlush(p1);

        Problem p2 = new Problem();
        p2.setTitle("Problem 2");
        problemRepository.saveAndFlush(p2);

        cp1 = new CompetitionProblem();
        cp1.setCompetition(competition);
        cp1.setProblem(p1);
        competitionProblemRepository.saveAndFlush(cp1);

        cp2 = new CompetitionProblem();
        cp2.setCompetition(competition);
        cp2.setProblem(p2);
        competitionProblemRepository.saveAndFlush(cp2);

        // --- Data Setup ---

        // User 1: cp1 improvement (20 -> 50). Delta = 30.
        createSubmission(user1, cp1, 20, wayPast);
        createSubmission(user1, cp1, 50, afterPast);

        // User 2: cp1 new submission (40). Delta = 40.
        createSubmission(user2, cp1, 40, afterPast);

        // User 3: cp1 no improvement (30 -> 30). Delta = 0.
        createSubmission(user3, cp1, 30, wayPast);
        createSubmission(user3, cp1, 25, afterPast); 

        // User 4 (EXCLUDED): cp1 improvement (10 -> 100). Delta ignored.
        User user4 = userRepository.findById(4L).get();
        createSubmission(user4, cp1, 10, wayPast);
        createSubmission(user4, cp1, 100, afterPast);

        // User 2032 (EXCLUDED): cp1 (delta 100) ignored.
        User user2032 = userRepository.findById(2032L).get();
        createSubmission(user2032, cp1, 100, afterPast);
        
        // User 6: cp2 submission (10). 
        User user6 = userRepository.findById(6L).get();
        createSubmission(user6, cp2, 10, afterPast);
    }

    private void insertUser(Long id, String login, String firstName, String lastName) {
        em.createNativeQuery("INSERT INTO jhi_user (id, login, password_hash, activated, created_by, first_name, last_name) " +
            "VALUES (?, ?, 'passwordpasswordpasswordpasswordpasswordpasswordpasswordpass', true, 'system', ?, ?)")
            .setParameter(1, id)
            .setParameter(2, login)
            .setParameter(3, firstName)
            .setParameter(4, lastName)
            .executeUpdate();
    }

    private void createSubmission(User user, CompetitionProblem cp, int points, ZonedDateTime date) {
        Submission s = new Submission();
        s.setUser(user);
        s.setCompetitionProblem(cp);
        s.setPoints(points);
        s.setUploadDate(date);
        submissionRepository.saveAndFlush(s);
    }

    private void assertRow(Object[] row, Long expectedId, String expectedFn, String expectedLn, Double expectedPoints) {
        assertThat(row[0].toString()).isEqualTo(expectedId.toString());
        assertThat(row[1]).isEqualTo(expectedFn);
        assertThat(row[2]).isEqualTo(expectedLn);
        assertThat(Double.valueOf(row[3].toString())).isEqualTo(expectedPoints);
    }

    @Test
    public void testGetRootStandings_FiltersExclusionsAndCalculatesDeltas() {
        List<Object[]> standings = competitionRepository.getRootStandings(past, 0, 10);
        
        assertThat(standings).hasSize(4);
        
        // Row 0: User 2 (40 pts)
        assertRow(standings.get(0), 2L, "Fn2", "Ln2", 40.0);

        // Row 1: User 1 (30 pts)
        assertRow(standings.get(1), 1L, "Fn1", "Ln1", 30.0);
        
        // Row 2: User 6 (10 pts)
        assertRow(standings.get(2), 6L, "Fn6", "Ln6", 10.0);

        // Row 3: User 3 (0 pts)
        assertRow(standings.get(3), 3L, "Fn3", "Ln3", 0.0);
    }

    @Test
    public void testGetRootStandings_Pagination() {
        List<Object[]> page1 = competitionRepository.getRootStandings(past, 0, 1);
        assertThat(page1).hasSize(1);
        assertRow(page1.get(0), 2L, "Fn2", "Ln2", 40.0);

        List<Object[]> page2 = competitionRepository.getRootStandings(past, 1, 1);
        assertThat(page2).hasSize(1);
        assertRow(page2.get(0), 1L, "Fn1", "Ln1", 30.0);
    }

    @Test
    public void testGetRootStandingsSize_OnlyCountsRecentSubmissions() {
        Integer size = competitionRepository.getRootStandingsSize(past);
        assertThat(size).isEqualTo(4);
    }

    @Test
    public void testGetAggregatedStandingsForProblems_FiltersByProblem() {
        List<Long> onlyCp2 = Arrays.asList(cp2.getId());
        List<Object[]> standings = competitionRepository.getAggregatedStandingsForProblems(past, onlyCp2, 0, 10);
        
        assertThat(standings).hasSize(1);
        assertRow(standings.get(0), 6L, "Fn6", "Ln6", 10.0);
    }

    @Test
    public void testGetAggregatedUserPointsForProblems() {
        List<Long> allCps = Arrays.asList(cp1.getId(), cp2.getId());
        List<Object[]> points = competitionRepository.getAggregatedUserPointsForProblems(past, allCps, user1.getId());
        
        assertThat(points).hasSize(1);
        assertRow(points.get(0), 1L, "Fn1", "Ln1", 30.0);
    }

    @Test
    public void testGetSimpleUserPointsPerProblem_NoDelta() {
        List<Long> allCps = Arrays.asList(cp1.getId(), cp2.getId());
        List<Object[]> points = competitionRepository.getSimpleUserPointsPerProblem(allCps, user1.getId());
        
        assertThat(points).hasSize(1);
        assertThat(points.get(0)[0].toString()).isEqualTo(cp1.getId().toString());
        assertThat(points.get(0)[1].toString()).isEqualTo("50");
    }

    @Test
    public void testGetStandingsPerProblemForProblems_DetailedJson() {
        List<Long> allCps = Arrays.asList(cp1.getId(), cp2.getId());
        List<Object[]> standings = competitionRepository.getStandingsPerProblemForProblems(past, allCps, 0, 10);
        
        assertThat(standings).hasSize(4);
        // User 1 is at index 1
        assertRow(standings.get(1), 1L, "Fn1", "Ln1", 30.0);
        String json = standings.get(1)[4].toString();
        assertThat(json).contains("\"" + cp1.getId() + "\":30");
    }

    @Test
    public void testGetUserPointsPerProblem() {
        List<Long> allCps = Arrays.asList(cp1.getId(), cp2.getId());
        List<Object[]> result = competitionRepository.getUserPointsPerProblem(past, allCps, user1.getId());
        
        assertThat(result).hasSize(1);
        assertRow(result.get(0), 1L, "Fn1", "Ln1", 30.0);
        assertThat(result.get(0)[4].toString()).contains("\"" + cp1.getId() + "\":30");
    }
    
    @Test
    public void testExclusions_ExplicitlyCheckIds4And2032() {
        List<Object[]> standings = competitionRepository.getRootStandings(past, 0, 100);
        for (Object[] row : standings) {
            long id = Long.parseLong(row[0].toString());
            assertThat(id).isNotEqualTo(4L);
            assertThat(id).isNotEqualTo(2032L);
        }
    }
}
