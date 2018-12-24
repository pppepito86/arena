package com.olimpiici.arena.web.rest;

import com.olimpiici.arena.ArenaApp;

import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.mapper.CompetitionProblemMapper;
import com.olimpiici.arena.web.rest.errors.ExceptionTranslator;

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
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;


import static com.olimpiici.arena.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the CompetitionProblemResource REST controller.
 *
 * @see CompetitionProblemResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ArenaApp.class)
public class CompetitionProblemResourceIntTest {

    private static final Integer DEFAULT_ORDER = 1;
    private static final Integer UPDATED_ORDER = 2;

    @Autowired
    private CompetitionProblemRepository competitionProblemRepository;

    @Autowired
    private CompetitionProblemMapper competitionProblemMapper;

    @Autowired
    private CompetitionProblemService competitionProblemService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restCompetitionProblemMockMvc;

    private CompetitionProblem competitionProblem;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final CompetitionProblemResource competitionProblemResource = new CompetitionProblemResource(competitionProblemService);
        this.restCompetitionProblemMockMvc = MockMvcBuilders.standaloneSetup(competitionProblemResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static CompetitionProblem createEntity(EntityManager em) {
        CompetitionProblem competitionProblem = new CompetitionProblem()
            .order(DEFAULT_ORDER);
        return competitionProblem;
    }

    @Before
    public void initTest() {
        competitionProblem = createEntity(em);
    }

    @Test
    @Transactional
    public void createCompetitionProblem() throws Exception {
        int databaseSizeBeforeCreate = competitionProblemRepository.findAll().size();

        // Create the CompetitionProblem
        CompetitionProblemDTO competitionProblemDTO = competitionProblemMapper.toDto(competitionProblem);
        restCompetitionProblemMockMvc.perform(post("/api/competition-problems")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(competitionProblemDTO)))
            .andExpect(status().isCreated());

        // Validate the CompetitionProblem in the database
        List<CompetitionProblem> competitionProblemList = competitionProblemRepository.findAll();
        assertThat(competitionProblemList).hasSize(databaseSizeBeforeCreate + 1);
        CompetitionProblem testCompetitionProblem = competitionProblemList.get(competitionProblemList.size() - 1);
        assertThat(testCompetitionProblem.getOrder()).isEqualTo(DEFAULT_ORDER);
    }

    @Test
    @Transactional
    public void createCompetitionProblemWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = competitionProblemRepository.findAll().size();

        // Create the CompetitionProblem with an existing ID
        competitionProblem.setId(1L);
        CompetitionProblemDTO competitionProblemDTO = competitionProblemMapper.toDto(competitionProblem);

        // An entity with an existing ID cannot be created, so this API call must fail
        restCompetitionProblemMockMvc.perform(post("/api/competition-problems")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(competitionProblemDTO)))
            .andExpect(status().isBadRequest());

        // Validate the CompetitionProblem in the database
        List<CompetitionProblem> competitionProblemList = competitionProblemRepository.findAll();
        assertThat(competitionProblemList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllCompetitionProblems() throws Exception {
        // Initialize the database
        competitionProblemRepository.saveAndFlush(competitionProblem);

        // Get all the competitionProblemList
        restCompetitionProblemMockMvc.perform(get("/api/competition-problems?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(competitionProblem.getId().intValue())))
            .andExpect(jsonPath("$.[*].order").value(hasItem(DEFAULT_ORDER)));
    }
    
    @Test
    @Transactional
    public void getCompetitionProblem() throws Exception {
        // Initialize the database
        competitionProblemRepository.saveAndFlush(competitionProblem);

        // Get the competitionProblem
        restCompetitionProblemMockMvc.perform(get("/api/competition-problems/{id}", competitionProblem.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(competitionProblem.getId().intValue()))
            .andExpect(jsonPath("$.order").value(DEFAULT_ORDER));
    }

    @Test
    @Transactional
    public void getNonExistingCompetitionProblem() throws Exception {
        // Get the competitionProblem
        restCompetitionProblemMockMvc.perform(get("/api/competition-problems/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateCompetitionProblem() throws Exception {
        // Initialize the database
        competitionProblemRepository.saveAndFlush(competitionProblem);

        int databaseSizeBeforeUpdate = competitionProblemRepository.findAll().size();

        // Update the competitionProblem
        CompetitionProblem updatedCompetitionProblem = competitionProblemRepository.findById(competitionProblem.getId()).get();
        // Disconnect from session so that the updates on updatedCompetitionProblem are not directly saved in db
        em.detach(updatedCompetitionProblem);
        updatedCompetitionProblem
            .order(UPDATED_ORDER);
        CompetitionProblemDTO competitionProblemDTO = competitionProblemMapper.toDto(updatedCompetitionProblem);

        restCompetitionProblemMockMvc.perform(put("/api/competition-problems")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(competitionProblemDTO)))
            .andExpect(status().isOk());

        // Validate the CompetitionProblem in the database
        List<CompetitionProblem> competitionProblemList = competitionProblemRepository.findAll();
        assertThat(competitionProblemList).hasSize(databaseSizeBeforeUpdate);
        CompetitionProblem testCompetitionProblem = competitionProblemList.get(competitionProblemList.size() - 1);
        assertThat(testCompetitionProblem.getOrder()).isEqualTo(UPDATED_ORDER);
    }

    @Test
    @Transactional
    public void updateNonExistingCompetitionProblem() throws Exception {
        int databaseSizeBeforeUpdate = competitionProblemRepository.findAll().size();

        // Create the CompetitionProblem
        CompetitionProblemDTO competitionProblemDTO = competitionProblemMapper.toDto(competitionProblem);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restCompetitionProblemMockMvc.perform(put("/api/competition-problems")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(competitionProblemDTO)))
            .andExpect(status().isBadRequest());

        // Validate the CompetitionProblem in the database
        List<CompetitionProblem> competitionProblemList = competitionProblemRepository.findAll();
        assertThat(competitionProblemList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteCompetitionProblem() throws Exception {
        // Initialize the database
        competitionProblemRepository.saveAndFlush(competitionProblem);

        int databaseSizeBeforeDelete = competitionProblemRepository.findAll().size();

        // Get the competitionProblem
        restCompetitionProblemMockMvc.perform(delete("/api/competition-problems/{id}", competitionProblem.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<CompetitionProblem> competitionProblemList = competitionProblemRepository.findAll();
        assertThat(competitionProblemList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(CompetitionProblem.class);
        CompetitionProblem competitionProblem1 = new CompetitionProblem();
        competitionProblem1.setId(1L);
        CompetitionProblem competitionProblem2 = new CompetitionProblem();
        competitionProblem2.setId(competitionProblem1.getId());
        assertThat(competitionProblem1).isEqualTo(competitionProblem2);
        competitionProblem2.setId(2L);
        assertThat(competitionProblem1).isNotEqualTo(competitionProblem2);
        competitionProblem1.setId(null);
        assertThat(competitionProblem1).isNotEqualTo(competitionProblem2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CompetitionProblemDTO.class);
        CompetitionProblemDTO competitionProblemDTO1 = new CompetitionProblemDTO();
        competitionProblemDTO1.setId(1L);
        CompetitionProblemDTO competitionProblemDTO2 = new CompetitionProblemDTO();
        assertThat(competitionProblemDTO1).isNotEqualTo(competitionProblemDTO2);
        competitionProblemDTO2.setId(competitionProblemDTO1.getId());
        assertThat(competitionProblemDTO1).isEqualTo(competitionProblemDTO2);
        competitionProblemDTO2.setId(2L);
        assertThat(competitionProblemDTO1).isNotEqualTo(competitionProblemDTO2);
        competitionProblemDTO1.setId(null);
        assertThat(competitionProblemDTO1).isNotEqualTo(competitionProblemDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(competitionProblemMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(competitionProblemMapper.fromId(null)).isNull();
    }
}
