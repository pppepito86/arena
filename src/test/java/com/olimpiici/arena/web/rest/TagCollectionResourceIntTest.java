package com.olimpiici.arena.web.rest;

import com.olimpiici.arena.ArenaApp;

import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.repository.TagCollectionRepository;
import com.olimpiici.arena.service.TagCollectionService;
import com.olimpiici.arena.service.dto.TagCollectionDTO;
import com.olimpiici.arena.service.mapper.TagCollectionMapper;
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
 * Test class for the TagCollectionResource REST controller.
 *
 * @see TagCollectionResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ArenaApp.class)
public class TagCollectionResourceIntTest {

    @Autowired
    private TagCollectionRepository tagCollectionRepository;

    @Autowired
    private TagCollectionMapper tagCollectionMapper;

    @Autowired
    private TagCollectionService tagCollectionService;

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

    private MockMvc restTagCollectionMockMvc;

    private TagCollection tagCollection;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final TagCollectionResource tagCollectionResource = new TagCollectionResource(tagCollectionService);
        this.restTagCollectionMockMvc = MockMvcBuilders.standaloneSetup(tagCollectionResource)
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
    public static TagCollection createEntity(EntityManager em) {
        TagCollection tagCollection = new TagCollection();
        return tagCollection;
    }

    @Before
    public void initTest() {
        tagCollection = createEntity(em);
    }

    @Test
    @Transactional
    public void createTagCollection() throws Exception {
        int databaseSizeBeforeCreate = tagCollectionRepository.findAll().size();

        // Create the TagCollection
        TagCollectionDTO tagCollectionDTO = tagCollectionMapper.toDto(tagCollection);
        restTagCollectionMockMvc.perform(post("/api/tag-collections")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tagCollectionDTO)))
            .andExpect(status().isCreated());

        // Validate the TagCollection in the database
        List<TagCollection> tagCollectionList = tagCollectionRepository.findAll();
        assertThat(tagCollectionList).hasSize(databaseSizeBeforeCreate + 1);
        TagCollection testTagCollection = tagCollectionList.get(tagCollectionList.size() - 1);
    }

    @Test
    @Transactional
    public void createTagCollectionWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = tagCollectionRepository.findAll().size();

        // Create the TagCollection with an existing ID
        tagCollection.setId(1L);
        TagCollectionDTO tagCollectionDTO = tagCollectionMapper.toDto(tagCollection);

        // An entity with an existing ID cannot be created, so this API call must fail
        restTagCollectionMockMvc.perform(post("/api/tag-collections")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tagCollectionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the TagCollection in the database
        List<TagCollection> tagCollectionList = tagCollectionRepository.findAll();
        assertThat(tagCollectionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllTagCollections() throws Exception {
        // Initialize the database
        tagCollectionRepository.saveAndFlush(tagCollection);

        // Get all the tagCollectionList
        restTagCollectionMockMvc.perform(get("/api/tag-collections?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(tagCollection.getId().intValue())));
    }
    
    @Test
    @Transactional
    public void getTagCollection() throws Exception {
        // Initialize the database
        tagCollectionRepository.saveAndFlush(tagCollection);

        // Get the tagCollection
        restTagCollectionMockMvc.perform(get("/api/tag-collections/{id}", tagCollection.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(tagCollection.getId().intValue()));
    }

    @Test
    @Transactional
    public void getNonExistingTagCollection() throws Exception {
        // Get the tagCollection
        restTagCollectionMockMvc.perform(get("/api/tag-collections/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateTagCollection() throws Exception {
        // Initialize the database
        tagCollectionRepository.saveAndFlush(tagCollection);

        int databaseSizeBeforeUpdate = tagCollectionRepository.findAll().size();

        // Update the tagCollection
        TagCollection updatedTagCollection = tagCollectionRepository.findById(tagCollection.getId()).get();
        // Disconnect from session so that the updates on updatedTagCollection are not directly saved in db
        em.detach(updatedTagCollection);
        TagCollectionDTO tagCollectionDTO = tagCollectionMapper.toDto(updatedTagCollection);

        restTagCollectionMockMvc.perform(put("/api/tag-collections")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tagCollectionDTO)))
            .andExpect(status().isOk());

        // Validate the TagCollection in the database
        List<TagCollection> tagCollectionList = tagCollectionRepository.findAll();
        assertThat(tagCollectionList).hasSize(databaseSizeBeforeUpdate);
        TagCollection testTagCollection = tagCollectionList.get(tagCollectionList.size() - 1);
    }

    @Test
    @Transactional
    public void updateNonExistingTagCollection() throws Exception {
        int databaseSizeBeforeUpdate = tagCollectionRepository.findAll().size();

        // Create the TagCollection
        TagCollectionDTO tagCollectionDTO = tagCollectionMapper.toDto(tagCollection);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTagCollectionMockMvc.perform(put("/api/tag-collections")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tagCollectionDTO)))
            .andExpect(status().isBadRequest());

        // Validate the TagCollection in the database
        List<TagCollection> tagCollectionList = tagCollectionRepository.findAll();
        assertThat(tagCollectionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteTagCollection() throws Exception {
        // Initialize the database
        tagCollectionRepository.saveAndFlush(tagCollection);

        int databaseSizeBeforeDelete = tagCollectionRepository.findAll().size();

        // Get the tagCollection
        restTagCollectionMockMvc.perform(delete("/api/tag-collections/{id}", tagCollection.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<TagCollection> tagCollectionList = tagCollectionRepository.findAll();
        assertThat(tagCollectionList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(TagCollection.class);
        TagCollection tagCollection1 = new TagCollection();
        tagCollection1.setId(1L);
        TagCollection tagCollection2 = new TagCollection();
        tagCollection2.setId(tagCollection1.getId());
        assertThat(tagCollection1).isEqualTo(tagCollection2);
        tagCollection2.setId(2L);
        assertThat(tagCollection1).isNotEqualTo(tagCollection2);
        tagCollection1.setId(null);
        assertThat(tagCollection1).isNotEqualTo(tagCollection2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(TagCollectionDTO.class);
        TagCollectionDTO tagCollectionDTO1 = new TagCollectionDTO();
        tagCollectionDTO1.setId(1L);
        TagCollectionDTO tagCollectionDTO2 = new TagCollectionDTO();
        assertThat(tagCollectionDTO1).isNotEqualTo(tagCollectionDTO2);
        tagCollectionDTO2.setId(tagCollectionDTO1.getId());
        assertThat(tagCollectionDTO1).isEqualTo(tagCollectionDTO2);
        tagCollectionDTO2.setId(2L);
        assertThat(tagCollectionDTO1).isNotEqualTo(tagCollectionDTO2);
        tagCollectionDTO1.setId(null);
        assertThat(tagCollectionDTO1).isNotEqualTo(tagCollectionDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(tagCollectionMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(tagCollectionMapper.fromId(null)).isNull();
    }
}
