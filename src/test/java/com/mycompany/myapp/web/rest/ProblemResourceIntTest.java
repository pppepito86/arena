package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.ArenaApp;

import com.mycompany.myapp.domain.Problem;
import com.mycompany.myapp.repository.ProblemRepository;
import com.mycompany.myapp.service.ProblemService;
import com.mycompany.myapp.service.dto.ProblemDTO;
import com.mycompany.myapp.service.mapper.ProblemMapper;
import com.mycompany.myapp.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;


import static com.mycompany.myapp.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the ProblemResource REST controller.
 *
 * @see ProblemResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ArenaApp.class)
public class ProblemResourceIntTest {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_DIRECTORY = "AAAAAAAAAA";
    private static final String UPDATED_DIRECTORY = "BBBBBBBBBB";

    private static final Integer DEFAULT_VERSION = 1;
    private static final Integer UPDATED_VERSION = 2;

    @Autowired
    private ProblemRepository problemRepository;

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private ProblemService problemService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restProblemMockMvc;

    private Problem problem;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final ProblemResource problemResource = new ProblemResource(problemService);
        this.restProblemMockMvc = MockMvcBuilders.standaloneSetup(problemResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Problem createEntity(EntityManager em) {
        Problem problem = new Problem()
            .title(DEFAULT_TITLE)
            .directory(DEFAULT_DIRECTORY)
            .version(DEFAULT_VERSION);
        return problem;
    }

    @Before
    public void initTest() {
        problem = createEntity(em);
    }

    @Test
    @Transactional
    public void createProblem() throws Exception {
        int databaseSizeBeforeCreate = problemRepository.findAll().size();

        // Create the Problem
        ProblemDTO problemDTO = problemMapper.toDto(problem);
        restProblemMockMvc.perform(post("/api/problems")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(problemDTO)))
            .andExpect(status().isCreated());

        // Validate the Problem in the database
        List<Problem> problemList = problemRepository.findAll();
        assertThat(problemList).hasSize(databaseSizeBeforeCreate + 1);
        Problem testProblem = problemList.get(problemList.size() - 1);
        assertThat(testProblem.getTitle()).isEqualTo(DEFAULT_TITLE);
        assertThat(testProblem.getDirectory()).isEqualTo(DEFAULT_DIRECTORY);
        assertThat(testProblem.getVersion()).isEqualTo(DEFAULT_VERSION);
    }

    @Test
    @Transactional
    public void createProblemWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = problemRepository.findAll().size();

        // Create the Problem with an existing ID
        problem.setId(1L);
        ProblemDTO problemDTO = problemMapper.toDto(problem);

        // An entity with an existing ID cannot be created, so this API call must fail
        restProblemMockMvc.perform(post("/api/problems")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(problemDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Problem in the database
        List<Problem> problemList = problemRepository.findAll();
        assertThat(problemList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllProblems() throws Exception {
        // Initialize the database
        problemRepository.saveAndFlush(problem);

        // Get all the problemList
        restProblemMockMvc.perform(get("/api/problems?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(problem.getId().intValue())))
            .andExpect(jsonPath("$.[*].title").value(hasItem(DEFAULT_TITLE.toString())))
            .andExpect(jsonPath("$.[*].directory").value(hasItem(DEFAULT_DIRECTORY.toString())))
            .andExpect(jsonPath("$.[*].version").value(hasItem(DEFAULT_VERSION)));
    }
    
    @Test
    @Transactional
    public void getProblem() throws Exception {
        // Initialize the database
        problemRepository.saveAndFlush(problem);

        // Get the problem
        restProblemMockMvc.perform(get("/api/problems/{id}", problem.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(problem.getId().intValue()))
            .andExpect(jsonPath("$.title").value(DEFAULT_TITLE.toString()))
            .andExpect(jsonPath("$.directory").value(DEFAULT_DIRECTORY.toString()))
            .andExpect(jsonPath("$.version").value(DEFAULT_VERSION));
    }

    @Test
    @Transactional
    public void getNonExistingProblem() throws Exception {
        // Get the problem
        restProblemMockMvc.perform(get("/api/problems/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateProblem() throws Exception {
        // Initialize the database
        problemRepository.saveAndFlush(problem);

        int databaseSizeBeforeUpdate = problemRepository.findAll().size();

        // Update the problem
        Problem updatedProblem = problemRepository.findById(problem.getId()).get();
        // Disconnect from session so that the updates on updatedProblem are not directly saved in db
        em.detach(updatedProblem);
        updatedProblem
            .title(UPDATED_TITLE)
            .directory(UPDATED_DIRECTORY)
            .version(UPDATED_VERSION);
        ProblemDTO problemDTO = problemMapper.toDto(updatedProblem);

        restProblemMockMvc.perform(put("/api/problems")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(problemDTO)))
            .andExpect(status().isOk());

        // Validate the Problem in the database
        List<Problem> problemList = problemRepository.findAll();
        assertThat(problemList).hasSize(databaseSizeBeforeUpdate);
        Problem testProblem = problemList.get(problemList.size() - 1);
        assertThat(testProblem.getTitle()).isEqualTo(UPDATED_TITLE);
        assertThat(testProblem.getDirectory()).isEqualTo(UPDATED_DIRECTORY);
        assertThat(testProblem.getVersion()).isEqualTo(UPDATED_VERSION);
    }

    @Test
    @Transactional
    public void updateNonExistingProblem() throws Exception {
        int databaseSizeBeforeUpdate = problemRepository.findAll().size();

        // Create the Problem
        ProblemDTO problemDTO = problemMapper.toDto(problem);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProblemMockMvc.perform(put("/api/problems")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(problemDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Problem in the database
        List<Problem> problemList = problemRepository.findAll();
        assertThat(problemList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteProblem() throws Exception {
        // Initialize the database
        problemRepository.saveAndFlush(problem);

        int databaseSizeBeforeDelete = problemRepository.findAll().size();

        // Get the problem
        restProblemMockMvc.perform(delete("/api/problems/{id}", problem.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Problem> problemList = problemRepository.findAll();
        assertThat(problemList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Problem.class);
        Problem problem1 = new Problem();
        problem1.setId(1L);
        Problem problem2 = new Problem();
        problem2.setId(problem1.getId());
        assertThat(problem1).isEqualTo(problem2);
        problem2.setId(2L);
        assertThat(problem1).isNotEqualTo(problem2);
        problem1.setId(null);
        assertThat(problem1).isNotEqualTo(problem2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ProblemDTO.class);
        ProblemDTO problemDTO1 = new ProblemDTO();
        problemDTO1.setId(1L);
        ProblemDTO problemDTO2 = new ProblemDTO();
        assertThat(problemDTO1).isNotEqualTo(problemDTO2);
        problemDTO2.setId(problemDTO1.getId());
        assertThat(problemDTO1).isEqualTo(problemDTO2);
        problemDTO2.setId(2L);
        assertThat(problemDTO1).isNotEqualTo(problemDTO2);
        problemDTO1.setId(null);
        assertThat(problemDTO1).isNotEqualTo(problemDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(problemMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(problemMapper.fromId(null)).isNull();
    }
}
