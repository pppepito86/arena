package com.olimpiici.arena.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.CompetitionRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.service.mapper.SubmissionMapper;
import com.olimpiici.arena.service.mapper.TagMapper;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service ementation for managing Submission.
 */
@Service
@Transactional
public class SubmissionService {

    private final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository submissionRepository;
    
    private final UserRepository userRepository;
    
    private final CompetitionRepository competitionRepository;
    
    private final CompetitionProblemRepository competitionProblemRepository;

    private final SubmissionMapper submissionMapper;
    
    private final ApplicationProperties applicationProperties;
    
    private final TagService tagService;

    private final TagMapper tagMapper;
    
    public SubmissionService(SubmissionRepository submissionRepository, 
    		SubmissionMapper submissionMapper,
    		CompetitionProblemRepository competitionProblemRepository,
    		UserRepository userRepository,
    		CompetitionRepository competitionRepository,
    		ApplicationProperties applicationProperties,
    		TagService tagService,
    		TagMapper tagMapper) {
        this.submissionRepository = submissionRepository;
        this.submissionMapper = submissionMapper;
        this.competitionProblemRepository = competitionProblemRepository;
        this.userRepository = userRepository;
        this.competitionRepository = competitionRepository;
        this.applicationProperties = applicationProperties;
        this.tagService = tagService;
        this.tagMapper = tagMapper;
    }

    /**
     * Save a submission.
     *
     * @param submissionDTO the entity to save
     * @return the persisted entity
     */
    
    public SubmissionDTO save(SubmissionDTO submissionDTO) {
        log.debug("Request to save Submission : {}", submissionDTO);

        Submission submission = submissionMapper.toEntity(submissionDTO);
        submission = submissionRepository.save(submission);
        return submissionMapper.toDto(submission);
    }

    /**
     * Get all the submissions.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    
    @Transactional(readOnly = true)
    public Page<SubmissionDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Submissions");
        return submissionRepository.findAll(pageable)
            .map(submissionMapper::toDto);
    }
    
    
	public Page<SubmissionDTO> findByUser(User user, Pageable pageable) {
		return submissionRepository.findByUser(user, pageable)
				.map(submissionMapper::toDto);
	}


    /**
     * Get one submission by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    
    @Transactional(readOnly = true)
    public Optional<SubmissionDTO> findOne(Long id) {
        log.debug("Request to get Submission : {}", id);
        return submissionRepository.findById(id)
            .map(submissionMapper::toDto)
            .map(dto -> {
            	dto.setCode(findSubmissionCode(id));
            	return dto;
            });
    }

    /**
     * Delete the submission by id.
     *
     * @param id the id of the entity
     */
    
    public void delete(Long id) {
        log.debug("Request to delete Submission : {}", id);
        submissionRepository.deleteById(id);
    }

	
	public Page<SubmissionDTO> findSubmissionsByCompetitionProblemAndUser(Long userId, 
			Long competitionProblemId, Pageable pageable) {
		log.debug("Request to get all submissions for CompetitionProblem {}", competitionProblemId);
		User user = userRepository.findById(userId).get();
		return findSubmissionsByCompetitionProblemAndUser(user, competitionProblemId, pageable);
	}
	
	
	public Page<SubmissionDTO> findSubmissionsByCompetitionProblemAndUser(User user, 
			Long competitionProblemId,
			Pageable pageable) {
		CompetitionProblem competitionProblem = competitionProblemRepository
				.findById(competitionProblemId).get();
		
		Page<SubmissionDTO> submissions = submissionRepository
				.findByCompetitionProblemAndUser(competitionProblem, user, pageable)
				.map(submissionMapper::toDto);
		return submissions;
	}
	
	
	public Page<SubmissionDTO> findSubmissionsByUserAndCompetitionProblemIn(User user,
			List<CompetitionProblem> competitionProblems, Pageable pageable) {
		return submissionRepository
				.findByUserAndCompetitionProblemIn(user, competitionProblems, pageable)
				.map(submissionMapper::toDto);
	}

	
	public Page<SubmissionDTO> findSubmissionsByCompetitionProblem(
			Long competitionProblemId, Pageable pageable) {
		CompetitionProblem competitionProblem = 
				competitionProblemRepository.getOne(competitionProblemId);
		Page<SubmissionDTO> submissions = submissionRepository
				.findByCompetitionProblem(competitionProblem, pageable)
				.map(submissionMapper::toDto);
		return submissions;
	}
	
	
	public Page<SubmissionDTO> findSubmissionsByCompetitionProblemIn(
			List<CompetitionProblem> competitionProblems, Pageable pageable) {
		return submissionRepository
				.findByCompetitionProblemIn(competitionProblems, pageable)
				.map(submissionMapper::toDto);
	}
	
	
	public List<SubmissionDTO> findSubmissionByVerdict(String verdict) {
		return submissionRepository
				.findByVerdict(verdict)
				.stream()
				.map(submissionMapper::toDto)
				.collect(Collectors.toList());
	}
	
	public String findSubmissionCode(Long id) {
		Optional<File> file = findSubmissionFile(applicationProperties.getWorkDir(), id);
		if (!file.isPresent()) {
			log.error("No submission files");
			return null;
		}

		try {
			return FileUtils.readFileToString(file.get(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Problem reading solution file source", e);
			return null;
		}
	}

	public static Optional<File> findSubmissionFile(String workdir, Long id) {
		File submissionDir = Paths.get(workdir, "submissions", ""+id).toFile();
		File[] filesInSubmssionDir = submissionDir.listFiles();
		if (filesInSubmssionDir == null) {
			return Optional.empty();
		}

		return Arrays.stream(filesInSubmssionDir)
			.filter(File::isFile)
			.filter(f -> f.getName().startsWith("solution"))
			.findFirst();
	}
    
    public List<TagDTO> findTags(Long id) {
    	Submission submission = submissionRepository.getOne(id);
    	return tagService.findTagsForCollection(submission.getTags())
	    	.map(tagMapper::toDto)
			.collect(Collectors.toList());
    }
    
    
    public void updateTags(Long id, List<TagDTO> newTags) {
    	Submission submission = submissionRepository.getOne(id);
    	TagCollection newCollection	= 
    			tagService.updateTagsForCollection(submission.getTags(), newTags);
    	
    	if (submission.getTags() == null) {
    		submission.setTags(newCollection);
    		submissionRepository.save(submission);
    	}
    }

	
	public void rejudge(Long id) {
		Submission submission = submissionRepository.getOne(id);
		submission.setVerdict("waiting");
		submissionRepository.save(submission);
	}

}
