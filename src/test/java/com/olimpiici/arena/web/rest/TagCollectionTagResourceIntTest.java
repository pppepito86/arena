package com.olimpiici.arena.web.rest;

import com.olimpiici.arena.ArenaApp;

import com.olimpiici.arena.domain.TagCollectionTag;
import com.olimpiici.arena.repository.TagCollectionTagRepository;
import com.olimpiici.arena.service.TagCollectionTagService;
import com.olimpiici.arena.service.dto.TagCollectionTagDTO;
import com.olimpiici.arena.service.mapper.TagCollectionTagMapper;
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
 * Test class for the TagCollectionTagResource REST controller.
 *
 * @see TagCollectionTagResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ArenaApp.class)
public class TagCollectionTagResourceIntTest {

    @Autowired
    private TagCollectionTagRepository tagCollectionTagRepository;

    @Autowired
    private TagCollectionTagMapper tagCollectionTagMapper;

    @Autowired
    private TagCollectionTagService tagCollectionTagService;

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

    private MockMvc restTagCollectionTagMockMvc;

    private TagCollectionTag tagCollectionTag;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final TagCollectionTagResource tagCollectionTagResource = new TagCollectionTagResource(tagCollectionTagService);
        this.restTagCollectionTagMockMvc = MockMvcBuilders.standaloneSetup(tagCollectionTagResource)
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
    public static TagCollectionTag createEntity(EntityManager em) {
        TagCollectionTag tagCollectionTag = new TagCollectionTag();
        return tagCollectionTag;
    }

    @Before
    public void initTest() {
        tagCollectionTag = createEntity(em);
    }

    @Test
    @Transactional
    public void createTagCollectionTag() throws Exception {
        int databaseSizeBeforeCreate = tagCollectionTagRepository.findAll().size();

        // Create the TagCollectionTag
        TagCollectionTagDTO tagCollectionTagDTO = tagCollectionTagMapper.toDto(tagCollectionTag);
        restTagCollectionTagMockMvc.perform(post("/api/tag-collection-tags")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tagCollectionTagDTO)))
            .andExpect(status().isCreated());

        // Validate the TagCollectionTag in the database
        List<TagCollectionTag> tagCollectionTagList = tagCollectionTagRepository.findAll();
        assertThat(tagCollectionTagList).hasSize(databaseSizeBeforeCreate + 1);
        TagCollectionTag testTagCollectionTag = tagCollectionTagList.get(tagCollectionTagList.size() - 1);
    }

    @Test
    @Transactional
    public void createTagCollectionTagWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = tagCollectionTagRepository.findAll().size();

        // Create the TagCollectionTag with an existing ID
        tagCollectionTag.setId(1L);
        TagCollectionTagDTO tagCollectionTagDTO = tagCollectionTagMapper.toDto(tagCollectionTag);

        // An entity with an existing ID cannot be created, so this API call must fail
        restTagCollectionTagMockMvc.perform(post("/api/tag-collection-tags")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tagCollectionTagDTO)))
            .andExpect(status().isBadRequest());

        // Validate the TagCollectionTag in the database
        List<TagCollectionTag> tagCollectionTagList = tagCollectionTagRepository.findAll();
        assertThat(tagCollectionTagList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllTagCollectionTags() throws Exception {
        // Initialize the database
        tagCollectionTagRepository.saveAndFlush(tagCollectionTag);

        // Get all the tagCollectionTagList
        restTagCollectionTagMockMvc.perform(get("/api/tag-collection-tags?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(tagCollectionTag.getId().intValue())));
    }
    
    @Test
    @Transactional
    public void getTagCollectionTag() throws Exception {
        // Initialize the database
        tagCollectionTagRepository.saveAndFlush(tagCollectionTag);

        // Get the tagCollectionTag
        restTagCollectionTagMockMvc.perform(get("/api/tag-collection-tags/{id}", tagCollectionTag.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(tagCollectionTag.getId().intValue()));
    }

    @Test
    @Transactional
    public void getNonExistingTagCollectionTag() throws Exception {
        // Get the tagCollectionTag
        restTagCollectionTagMockMvc.perform(get("/api/tag-collection-tags/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateTagCollectionTag() throws Exception {
        // Initialize the database
        tagCollectionTagRepository.saveAndFlush(tagCollectionTag);

        int databaseSizeBeforeUpdate = tagCollectionTagRepository.findAll().size();

        // Update the tagCollectionTag
        TagCollectionTag updatedTagCollectionTag = tagCollectionTagRepository.findById(tagCollectionTag.getId()).get();
        // Disconnect from session so that the updates on updatedTagCollectionTag are not directly saved in db
        em.detach(updatedTagCollectionTag);
        TagCollectionTagDTO tagCollectionTagDTO = tagCollectionTagMapper.toDto(updatedTagCollectionTag);

        restTagCollectionTagMockMvc.perform(put("/api/tag-collection-tags")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tagCollectionTagDTO)))
            .andExpect(status().isOk());

        // Validate the TagCollectionTag in the database
        List<TagCollectionTag> tagCollectionTagList = tagCollectionTagRepository.findAll();
        assertThat(tagCollectionTagList).hasSize(databaseSizeBeforeUpdate);
        TagCollectionTag testTagCollectionTag = tagCollectionTagList.get(tagCollectionTagList.size() - 1);
    }

    @Test
    @Transactional
    public void updateNonExistingTagCollectionTag() throws Exception {
        int databaseSizeBeforeUpdate = tagCollectionTagRepository.findAll().size();

        // Create the TagCollectionTag
        TagCollectionTagDTO tagCollectionTagDTO = tagCollectionTagMapper.toDto(tagCollectionTag);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restTagCollectionTagMockMvc.perform(put("/api/tag-collection-tags")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tagCollectionTagDTO)))
            .andExpect(status().isBadRequest());

        // Validate the TagCollectionTag in the database
        List<TagCollectionTag> tagCollectionTagList = tagCollectionTagRepository.findAll();
        assertThat(tagCollectionTagList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteTagCollectionTag() throws Exception {
        // Initialize the database
        tagCollectionTagRepository.saveAndFlush(tagCollectionTag);

        int databaseSizeBeforeDelete = tagCollectionTagRepository.findAll().size();

        // Get the tagCollectionTag
        restTagCollectionTagMockMvc.perform(delete("/api/tag-collection-tags/{id}", tagCollectionTag.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<TagCollectionTag> tagCollectionTagList = tagCollectionTagRepository.findAll();
        assertThat(tagCollectionTagList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(TagCollectionTag.class);
        TagCollectionTag tagCollectionTag1 = new TagCollectionTag();
        tagCollectionTag1.setId(1L);
        TagCollectionTag tagCollectionTag2 = new TagCollectionTag();
        tagCollectionTag2.setId(tagCollectionTag1.getId());
        assertThat(tagCollectionTag1).isEqualTo(tagCollectionTag2);
        tagCollectionTag2.setId(2L);
        assertThat(tagCollectionTag1).isNotEqualTo(tagCollectionTag2);
        tagCollectionTag1.setId(null);
        assertThat(tagCollectionTag1).isNotEqualTo(tagCollectionTag2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(TagCollectionTagDTO.class);
        TagCollectionTagDTO tagCollectionTagDTO1 = new TagCollectionTagDTO();
        tagCollectionTagDTO1.setId(1L);
        TagCollectionTagDTO tagCollectionTagDTO2 = new TagCollectionTagDTO();
        assertThat(tagCollectionTagDTO1).isNotEqualTo(tagCollectionTagDTO2);
        tagCollectionTagDTO2.setId(tagCollectionTagDTO1.getId());
        assertThat(tagCollectionTagDTO1).isEqualTo(tagCollectionTagDTO2);
        tagCollectionTagDTO2.setId(2L);
        assertThat(tagCollectionTagDTO1).isNotEqualTo(tagCollectionTagDTO2);
        tagCollectionTagDTO1.setId(null);
        assertThat(tagCollectionTagDTO1).isNotEqualTo(tagCollectionTagDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(tagCollectionTagMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(tagCollectionTagMapper.fromId(null)).isNull();
    }
}
