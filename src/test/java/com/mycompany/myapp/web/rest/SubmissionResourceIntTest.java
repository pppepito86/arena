package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.ArenaApp;

import com.mycompany.myapp.domain.Submission;
import com.mycompany.myapp.repository.SubmissionRepository;
import com.mycompany.myapp.service.SubmissionService;
import com.mycompany.myapp.service.dto.SubmissionDTO;
import com.mycompany.myapp.service.mapper.SubmissionMapper;
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.List;


import static com.mycompany.myapp.web.rest.TestUtil.sameInstant;
import static com.mycompany.myapp.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the SubmissionResource REST controller.
 *
 * @see SubmissionResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ArenaApp.class)
public class SubmissionResourceIntTest {

    private static final String DEFAULT_FILE = "AAAAAAAAAA";
    private static final String UPDATED_FILE = "BBBBBBBBBB";

    private static final String DEFAULT_VERDICT = "AAAAAAAAAA";
    private static final String UPDATED_VERDICT = "BBBBBBBBBB";

    private static final String DEFAULT_DETAILS = "AAAAAAAAAA";
    private static final String UPDATED_DETAILS = "BBBBBBBBBB";

    private static final Integer DEFAULT_POINTS = 1;
    private static final Integer UPDATED_POINTS = 2;

    private static final Integer DEFAULT_TIME_IN_MILLIS = 1;
    private static final Integer UPDATED_TIME_IN_MILLIS = 2;

    private static final Integer DEFAULT_MEMORY_IN_BYTES = 1;
    private static final Integer UPDATED_MEMORY_IN_BYTES = 2;

    private static final ZonedDateTime DEFAULT_UPLOAD_DATE = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_UPLOAD_DATE = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionMapper submissionMapper;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restSubmissionMockMvc;

    private Submission submission;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final SubmissionResource submissionResource = new SubmissionResource(submissionService);
        this.restSubmissionMockMvc = MockMvcBuilders.standaloneSetup(submissionResource)
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
    public static Submission createEntity(EntityManager em) {
        Submission submission = new Submission()
            .file(DEFAULT_FILE)
            .verdict(DEFAULT_VERDICT)
            .details(DEFAULT_DETAILS)
            .points(DEFAULT_POINTS)
            .timeInMillis(DEFAULT_TIME_IN_MILLIS)
            .memoryInBytes(DEFAULT_MEMORY_IN_BYTES)
            .uploadDate(DEFAULT_UPLOAD_DATE);
        return submission;
    }

    @Before
    public void initTest() {
        submission = createEntity(em);
    }

    @Test
    @Transactional
    public void createSubmission() throws Exception {
        int databaseSizeBeforeCreate = submissionRepository.findAll().size();

        // Create the Submission
        SubmissionDTO submissionDTO = submissionMapper.toDto(submission);
        restSubmissionMockMvc.perform(post("/api/submissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(submissionDTO)))
            .andExpect(status().isCreated());

        // Validate the Submission in the database
        List<Submission> submissionList = submissionRepository.findAll();
        assertThat(submissionList).hasSize(databaseSizeBeforeCreate + 1);
        Submission testSubmission = submissionList.get(submissionList.size() - 1);
        assertThat(testSubmission.getFile()).isEqualTo(DEFAULT_FILE);
        assertThat(testSubmission.getVerdict()).isEqualTo(DEFAULT_VERDICT);
        assertThat(testSubmission.getDetails()).isEqualTo(DEFAULT_DETAILS);
        assertThat(testSubmission.getPoints()).isEqualTo(DEFAULT_POINTS);
        assertThat(testSubmission.getTimeInMillis()).isEqualTo(DEFAULT_TIME_IN_MILLIS);
        assertThat(testSubmission.getMemoryInBytes()).isEqualTo(DEFAULT_MEMORY_IN_BYTES);
        assertThat(testSubmission.getUploadDate()).isEqualTo(DEFAULT_UPLOAD_DATE);
    }

    @Test
    @Transactional
    public void createSubmissionWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = submissionRepository.findAll().size();

        // Create the Submission with an existing ID
        submission.setId(1L);
        SubmissionDTO submissionDTO = submissionMapper.toDto(submission);

        // An entity with an existing ID cannot be created, so this API call must fail
        restSubmissionMockMvc.perform(post("/api/submissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(submissionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Submission in the database
        List<Submission> submissionList = submissionRepository.findAll();
        assertThat(submissionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllSubmissions() throws Exception {
        // Initialize the database
        submissionRepository.saveAndFlush(submission);

        // Get all the submissionList
        restSubmissionMockMvc.perform(get("/api/submissions?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(submission.getId().intValue())))
            .andExpect(jsonPath("$.[*].file").value(hasItem(DEFAULT_FILE.toString())))
            .andExpect(jsonPath("$.[*].verdict").value(hasItem(DEFAULT_VERDICT.toString())))
            .andExpect(jsonPath("$.[*].details").value(hasItem(DEFAULT_DETAILS.toString())))
            .andExpect(jsonPath("$.[*].points").value(hasItem(DEFAULT_POINTS)))
            .andExpect(jsonPath("$.[*].timeInMillis").value(hasItem(DEFAULT_TIME_IN_MILLIS)))
            .andExpect(jsonPath("$.[*].memoryInBytes").value(hasItem(DEFAULT_MEMORY_IN_BYTES)))
            .andExpect(jsonPath("$.[*].uploadDate").value(hasItem(sameInstant(DEFAULT_UPLOAD_DATE))));
    }
    
    @Test
    @Transactional
    public void getSubmission() throws Exception {
        // Initialize the database
        submissionRepository.saveAndFlush(submission);

        // Get the submission
        restSubmissionMockMvc.perform(get("/api/submissions/{id}", submission.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(submission.getId().intValue()))
            .andExpect(jsonPath("$.file").value(DEFAULT_FILE.toString()))
            .andExpect(jsonPath("$.verdict").value(DEFAULT_VERDICT.toString()))
            .andExpect(jsonPath("$.details").value(DEFAULT_DETAILS.toString()))
            .andExpect(jsonPath("$.points").value(DEFAULT_POINTS))
            .andExpect(jsonPath("$.timeInMillis").value(DEFAULT_TIME_IN_MILLIS))
            .andExpect(jsonPath("$.memoryInBytes").value(DEFAULT_MEMORY_IN_BYTES))
            .andExpect(jsonPath("$.uploadDate").value(sameInstant(DEFAULT_UPLOAD_DATE)));
    }

    @Test
    @Transactional
    public void getNonExistingSubmission() throws Exception {
        // Get the submission
        restSubmissionMockMvc.perform(get("/api/submissions/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateSubmission() throws Exception {
        // Initialize the database
        submissionRepository.saveAndFlush(submission);

        int databaseSizeBeforeUpdate = submissionRepository.findAll().size();

        // Update the submission
        Submission updatedSubmission = submissionRepository.findById(submission.getId()).get();
        // Disconnect from session so that the updates on updatedSubmission are not directly saved in db
        em.detach(updatedSubmission);
        updatedSubmission
            .file(UPDATED_FILE)
            .verdict(UPDATED_VERDICT)
            .details(UPDATED_DETAILS)
            .points(UPDATED_POINTS)
            .timeInMillis(UPDATED_TIME_IN_MILLIS)
            .memoryInBytes(UPDATED_MEMORY_IN_BYTES)
            .uploadDate(UPDATED_UPLOAD_DATE);
        SubmissionDTO submissionDTO = submissionMapper.toDto(updatedSubmission);

        restSubmissionMockMvc.perform(put("/api/submissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(submissionDTO)))
            .andExpect(status().isOk());

        // Validate the Submission in the database
        List<Submission> submissionList = submissionRepository.findAll();
        assertThat(submissionList).hasSize(databaseSizeBeforeUpdate);
        Submission testSubmission = submissionList.get(submissionList.size() - 1);
        assertThat(testSubmission.getFile()).isEqualTo(UPDATED_FILE);
        assertThat(testSubmission.getVerdict()).isEqualTo(UPDATED_VERDICT);
        assertThat(testSubmission.getDetails()).isEqualTo(UPDATED_DETAILS);
        assertThat(testSubmission.getPoints()).isEqualTo(UPDATED_POINTS);
        assertThat(testSubmission.getTimeInMillis()).isEqualTo(UPDATED_TIME_IN_MILLIS);
        assertThat(testSubmission.getMemoryInBytes()).isEqualTo(UPDATED_MEMORY_IN_BYTES);
        assertThat(testSubmission.getUploadDate()).isEqualTo(UPDATED_UPLOAD_DATE);
    }

    @Test
    @Transactional
    public void updateNonExistingSubmission() throws Exception {
        int databaseSizeBeforeUpdate = submissionRepository.findAll().size();

        // Create the Submission
        SubmissionDTO submissionDTO = submissionMapper.toDto(submission);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restSubmissionMockMvc.perform(put("/api/submissions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(submissionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Submission in the database
        List<Submission> submissionList = submissionRepository.findAll();
        assertThat(submissionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteSubmission() throws Exception {
        // Initialize the database
        submissionRepository.saveAndFlush(submission);

        int databaseSizeBeforeDelete = submissionRepository.findAll().size();

        // Get the submission
        restSubmissionMockMvc.perform(delete("/api/submissions/{id}", submission.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Submission> submissionList = submissionRepository.findAll();
        assertThat(submissionList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Submission.class);
        Submission submission1 = new Submission();
        submission1.setId(1L);
        Submission submission2 = new Submission();
        submission2.setId(submission1.getId());
        assertThat(submission1).isEqualTo(submission2);
        submission2.setId(2L);
        assertThat(submission1).isNotEqualTo(submission2);
        submission1.setId(null);
        assertThat(submission1).isNotEqualTo(submission2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SubmissionDTO.class);
        SubmissionDTO submissionDTO1 = new SubmissionDTO();
        submissionDTO1.setId(1L);
        SubmissionDTO submissionDTO2 = new SubmissionDTO();
        assertThat(submissionDTO1).isNotEqualTo(submissionDTO2);
        submissionDTO2.setId(submissionDTO1.getId());
        assertThat(submissionDTO1).isEqualTo(submissionDTO2);
        submissionDTO2.setId(2L);
        assertThat(submissionDTO1).isNotEqualTo(submissionDTO2);
        submissionDTO1.setId(null);
        assertThat(submissionDTO1).isNotEqualTo(submissionDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(submissionMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(submissionMapper.fromId(null)).isNull();
    }
}
