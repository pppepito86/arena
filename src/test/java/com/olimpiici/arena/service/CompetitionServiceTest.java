package com.olimpiici.arena.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.domain.UserPoints;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.CompetitionRepository;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.dto.CompetitionDTO;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.mapper.CompetitionMapper;
import com.olimpiici.arena.service.mapper.CompetitionProblemMapper;
import com.olimpiici.arena.service.mapper.ProblemMapper;
import com.olimpiici.arena.service.mapper.SubmissionMapper;

public class CompetitionServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;
    @Mock
    private CompetitionProblemRepository competitionProblemRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private CompetitionMapper competitionMapper;
    @Mock
    private ProblemMapper problemMapper;
    @Mock
    private CompetitionProblemMapper competitionProblemMapper;
    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProblemRepository problemRepository;

    private CompetitionService competitionService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        competitionService = new CompetitionService();
        ReflectionTestUtils.setField(competitionService, "competitionRepository", competitionRepository);
        ReflectionTestUtils.setField(competitionService, "competitionProblemRepository", competitionProblemRepository);
        ReflectionTestUtils.setField(competitionService, "submissionRepository", submissionRepository);
        ReflectionTestUtils.setField(competitionService, "competitionMapper", competitionMapper);
        ReflectionTestUtils.setField(competitionService, "problemMapper", problemMapper);
        ReflectionTestUtils.setField(competitionService, "competitionProblemMapper", competitionProblemMapper);
        ReflectionTestUtils.setField(competitionService, "submissionMapper", submissionMapper);
        ReflectionTestUtils.setField(competitionService, "userRepository", userRepository);
        ReflectionTestUtils.setField(competitionService, "problemRepository", problemRepository);
    }

    @Test
    public void testFindPathFromRoot() {
        Competition root = new Competition();
        root.setId(1L);
        Competition child = new Competition();
        child.setId(2L);
        child.setParent(root);

        CompetitionDTO rootDto = new CompetitionDTO();
        rootDto.setId(1L);
        CompetitionDTO childDto = new CompetitionDTO();
        childDto.setId(2L);
        childDto.setParentId(1L);

        when(competitionRepository.findById(2L)).thenReturn(Optional.of(child));
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(root));

        when(competitionMapper.toDto(any(Competition.class))).thenReturn(childDto, rootDto);

        List<CompetitionDTO> path = competitionService.findPathFromRoot(2L);

        assertThat(path).hasSize(2);
        assertThat(path.get(0).getId()).isEqualTo(1L);
        assertThat(path.get(1).getId()).isEqualTo(2L);
    }

    @Test
    public void testFindPointsForCompetitionProblem() {
        User user = new User();
        CompetitionProblem cp = new CompetitionProblem();

        Submission s1 = new Submission();
        s1.setPoints(20);
        Submission s2 = new Submission();
        s2.setPoints(50);

        when(submissionRepository.findByCompetitionProblemAndUser(cp, user))
            .thenReturn(Arrays.asList(s1, s2));

        Integer points = competitionService.findPointsForCompetitionProblem(user, cp);

        assertThat(points).isEqualTo(50);
    }

    @Test
    public void testFindTotalPoints() {
        User user = new User();
        CompetitionProblem cp1 = new CompetitionProblem();
        cp1.setId(1L);
        CompetitionProblem cp2 = new CompetitionProblem();
        cp2.setId(2L);

        Submission s1 = new Submission();
        s1.setCompetitionProblem(cp1);
        s1.setPoints(30);

        Submission s2 = new Submission();
        s2.setCompetitionProblem(cp2);
        s2.setPoints(40);

        when(submissionRepository.findByUser(user)).thenReturn(Arrays.asList(s1, s2));

        Integer total = competitionService.findTotalPoints(user);

        assertThat(total).isEqualTo(70);
    }

    @Test
    public void testFindStandings_RootOptimized() {
        ZonedDateTime from = ZonedDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);

        Object[] row = new Object[] { BigInteger.valueOf(1L), "Fn", "Ln", BigDecimal.valueOf(100.0), "{}" };
        List<Object[]> rawList = new ArrayList<>();
        rawList.add(row);

        when(competitionRepository.getRootStandings(from, 0, 10))
            .thenReturn(rawList);
        when(competitionRepository.getRootStandingsSize(from)).thenReturn(1);

        Page<UserPoints> standings = competitionService.findStandings(1L, pageable, from, null);

        assertThat(standings.getTotalElements()).isEqualTo(1);
        assertThat(standings.getContent().get(0).userId).isEqualTo(1L);
        assertThat(standings.getContent().get(0).points).isEqualTo(100);
    }

    @Test
    public void testFindAllCompetitionsInSubTree() {
        Competition root = new Competition();
        root.setId(1L);
        root.setLabel("Root");

        Competition child = new Competition();
        child.setId(2L);
        child.setLabel("Child");

        when(competitionRepository.findByParentIn(anyList()))
            .thenReturn(new ArrayList<>(Arrays.asList(child)))
            .thenReturn(new ArrayList<>())
            .thenReturn(new ArrayList<>());

        List<Competition> result = competitionService.findAllCompetitionsInSubTree(root);

        assertThat(result).contains(root, child);
    }

    @Test
    public void testUpdateSubCompetitions() {
        Competition parent = new Competition();
        parent.setId(1L);
        when(competitionRepository.getOne(1L)).thenReturn(parent);

        CompetitionDTO childDto = new CompetitionDTO();
        childDto.setId(2L);

        Competition child = new Competition();
        child.setId(2L);
        when(competitionRepository.getOne(2L)).thenReturn(child);

        when(competitionRepository.findByParent(eq(parent), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(child)));

        competitionService.updateSubCompetitions(1L, Arrays.asList(childDto));

        assertThat(child.getParent()).isEqualTo(parent);
        assertThat(child.getOrder()).isEqualTo(0);
        verify(competitionRepository).save(child);
    }

    @Test
    public void testFindProblems() {
        Competition competition = new Competition();
        competition.setId(1L);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        Problem problem = new Problem();
        problem.setTitle("Problem Title");
        CompetitionProblem cp = new CompetitionProblem();
        cp.setProblem(problem);

        when(competitionProblemRepository.findByCompetition(eq(competition), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cp)));

        CompetitionProblemDTO cpDto = new CompetitionProblemDTO();
        when(competitionProblemMapper.toDto(cp)).thenReturn(cpDto);

        Page<CompetitionProblemDTO> result = competitionService.findProblems(1L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Problem Title");
    }

    @Test
    public void testFindSimplePointsForUserPerProblem() {
        List<Long> cpIds = Arrays.asList(1L, 2L);
        Object[] row = new Object[] { BigInteger.valueOf(1L), 100 };
        List<Object[]> rawList = new ArrayList<>();
        rawList.add(row);

        when(competitionRepository.getSimpleUserPointsPerProblem(cpIds, 10L))
            .thenReturn(rawList);

        Map<Long, Integer> result = competitionService.findSimplePointsForUserPerProblem(cpIds, 10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isEqualTo(100);
        assertThat(result.get(2L)).isEqualTo(0); // Default value
    }

    @Test
    public void testUpdateSubProblems() {
        Competition parent = new Competition();
        parent.setId(1L);
        when(competitionRepository.getOne(1L)).thenReturn(parent);

        CompetitionProblemDTO cpDto = new CompetitionProblemDTO();
        cpDto.setId(100L);

        CompetitionProblem cp = new CompetitionProblem();
        cp.setId(100L);
        cp.setCompetition(parent); // Ensure competition is set to avoid NPE
        when(competitionProblemRepository.getOne(100L)).thenReturn(cp);
        when(competitionProblemRepository.save(any(CompetitionProblem.class))).thenReturn(cp);

        when(competitionProblemRepository.findByCompetition(eq(parent), any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(cp)));

        competitionService.updateSubProblems(1L, Arrays.asList(cpDto));

        assertThat(cp.getOrder()).isEqualTo(0);
        verify(competitionProblemRepository).save(cp);
    }
}
