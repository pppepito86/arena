package com.olimpiici.arena.service;

import com.olimpiici.arena.grader.WorkerPool;
import com.olimpiici.arena.service.ProblemService;
import com.olimpiici.arena.service.TagService;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.SubmissionService;
import com.fasterxml.jackson.databind.annotation.JsonAppend.Prop;
import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.repository.TagCollectionRepository;
import com.olimpiici.arena.repository.TagCollectionTagRepository;
import com.olimpiici.arena.repository.TagRepository;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.service.mapper.ProblemMapper;
import com.olimpiici.arena.service.mapper.TagMapper;
import com.olimpiici.arena.service.util.HomographTranslator;

import ch.qos.logback.core.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Service ementation for managing Problem.
 */
@Service
@Transactional
public class ProblemService {

    private final Logger log = LoggerFactory.getLogger(ProblemService.class);

    private final ProblemRepository problemRepository;

    private final ProblemMapper problemMapper;

    private final SubmissionService submissionService;

    private final CompetitionProblemService competitionProblemService;
    
    @Autowired
    private CompetitionProblemRepository competitionProblemRepository;

    private final TagCollectionTagRepository tagCollectionTagRepository;
    
    private final TagCollectionRepository tagCollectionRepository;

    private final TagRepository tagRepository;
    
    private final TagMapper tagMapper;
    
    private final TagService tagService;
    
    @Autowired
    private ApplicationProperties applicationProperties;
   
    @Autowired 
    private WorkerPool workerPool;

    public ProblemService(CompetitionProblemService competitionProblemService,
            ProblemRepository problemRepository, 
    		ProblemMapper problemMapper,
            SubmissionService submissionService,
    		TagCollectionTagRepository tagCollectionTagRepository,
    		TagCollectionRepository tagCollectionRepository,
    		TagRepository tagRepository,
    		TagMapper tagMapper,
    		TagService tagService) {
        this.problemRepository = problemRepository;
        this.problemMapper = problemMapper;
        this.competitionProblemService = competitionProblemService;
        this.submissionService = submissionService;
        this.tagCollectionTagRepository = tagCollectionTagRepository;
        this.tagCollectionRepository = tagCollectionRepository;
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
        this.tagService = tagService;
    }

    /**
     * Save a problem.
     *
     * @param problemDTO the entity to save
     * @return the persisted entity
     */
    
    public ProblemDTO save(ProblemDTO problemDTO) {
        log.debug("Request to save Problem : {}", problemDTO);

        Problem problem = problemMapper.toEntity(problemDTO);
        problem = problemRepository.save(problem);
        return problemMapper.toDto(problem);
    }

    /**
     * Get all the problems.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    
    @Transactional(readOnly = true)
    public Page<ProblemDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Problems");
        Page<ProblemDTO> page = problemRepository.findAll(pageable)
            .map(problemMapper::toDto);
               
        return page;
    }


    /**
     * Get one problem by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    
    @Transactional(readOnly = true)
    public Optional<ProblemDTO> findOne(Long id) {
        log.debug("Request to get Problem : {}", id);
        return problemRepository.findById(id)
            .map(problemMapper::toDto);
    }

    /**
     * Delete the problem by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Problem : {}", id);
        problemRepository.deleteById(id);
    }
    
    
    public List<TagDTO> findTags(Long id) {
    	Problem problem = problemRepository.getOne(id);
    	return findTags(problem.getTags());
    }
    
    public List<TagDTO> findTags(TagCollection tagCollection) {
    	return tagService.findTagsForCollection(tagCollection)
	    	.map(tagMapper::toDto)
			.collect(Collectors.toList());
    }
    
    
    public void updateTags(Long id, List<TagDTO> newTags) {
    	Problem problem = problemRepository.getOne(id);
    	TagCollection newCollection	= 
    			tagService.updateTagsForCollection(problem.getTags(), newTags);
    	
    	if (problem.getTags() == null) {
	    	problem.setTags(newCollection);
			problemRepository.save(problem);
    	}
    }

    
	public Properties getProperties(Long problemId) throws Exception {
    	Properties props = new Properties();
		File gradePropertiesFile = getGradeProperties(problemId);
		if (!gradePropertiesFile.exists()) {
			props.setProperty("time", "1");
			props.setProperty("memory", "256");
		} else {
			try (FileInputStream fis = new FileInputStream(gradePropertiesFile)) {
				props.load(fis);
			}
		}
		return props;
	}
  
    
    public void autoSetTimeLimits() throws Exception {
       PageRequest page = PageRequest.of(0, 10000);
       List<ProblemDTO> problems = findAll(page).getContent();
       log.debug("Setting time limit for " + problems.size() + " problems");
       for (ProblemDTO problem : problems) {
           autoSetTimeLimits(problem.getId());
       }
    }

	
	public void autoSetTimeLimits(Long id) throws Exception { 
        // TODO: use problem id, not comp problem id
        log.debug("Setting automatic time limit for problem " + id);
        PageRequest page = PageRequest.of(0, 10000);
        List<SubmissionDTO> submissions = submissionService
            .findSubmissionsByCompetitionProblem(id, page)
            .getContent();

        long problemId = competitionProblemService.findOne(id).get().getProblemId();

        final int authorUserId = 4;
        final int numSolutions = 3;

        boolean hasEnoughGoodSubmitions = submissions
                            .stream()
                            .filter(s -> s.getUserId() == authorUserId && s.getPoints() == 100)
                            .count() >= numSolutions;
    
        if (hasEnoughGoodSubmitions) {
            List<Integer> times = submissions.stream()
                    .filter(s -> s.getUserId() == authorUserId && s.getPoints() == 100)
                    .limit(numSolutions)
                    .map(s -> s.getTimeInMillis())
                    .collect(Collectors.toList());

            int max = times.stream().mapToInt(t -> t).max().getAsInt();

            int timeLimitMs = 100 * ((max*15/10)/100+1);

            log.info("limit for problem<" + problemId + "> with times " + 
                    times + " will be " + timeLimitMs + "ms");

            updateTimeLimit(problemId, timeLimitMs);
            workerPool.deleteProblem(problemId);
        } else {
            log.info("can't find " + numSolutions + " good solutions for problemId = " + problemId);
        }
	}

	
	public void updateTimeLimit(Long problemId, int newTimeLimitMs) throws Exception {
		String timeValue = newTimeLimitMs/1000 + "." + newTimeLimitMs%1000;
		
		Properties props = getProperties(problemId);
		props.setProperty("time", timeValue);
		writeGradeProperties(problemId, props);
		recreateProblemZip(problemId);
	}

	
	public void updateMemoryLimit(Long problemId, int newMemoryLimitMb) throws Exception {
		Properties props = getProperties(problemId);
		props.setProperty("memory", String.valueOf(newMemoryLimitMb));
		writeGradeProperties(problemId, props);
		recreateProblemZip(problemId);
	}

	private File getGradeProperties(long problemId) {
        String workdir = applicationProperties.getWorkDir(); 
		return Paths.get(workdir , "problems", String.valueOf(problemId), "problem", "grade.properties")
				.toFile();
	}
	
	private void writeGradeProperties(long problemId, Properties props) throws Exception {
		File gradePropertiesFile = getGradeProperties(problemId);
		if (!gradePropertiesFile.exists()) {
			gradePropertiesFile.createNewFile();
		}
		try (PrintWriter pw = new PrintWriter(gradePropertiesFile)) {
			props.store(pw, null);
		}
	}
	
	private void recreateProblemZip(long problemId) throws Exception {
        String workdir = applicationProperties.getWorkDir(); 
        File problemsDir = Paths.get(workdir, "problems", String.valueOf(problemId), "problem").toFile();
		ProcessExecutor executor = new ProcessExecutor()
      			.command("zip", "-r", "problem.zip", ".")
      			.directory(problemsDir);
      	executor.execute();
      	File problemZipNew = Paths
            .get(workdir, "problems", String.valueOf(problemId), "problem", "problem.zip")
            .toFile();
      	File problemZipOrig = Paths
            .get(workdir, "problems", String.valueOf(problemId), "problem.zip")
            .toFile();
      	problemZipOrig.delete();
      	problemZipNew.renameTo(problemZipOrig);
	}

	
	public ProblemDTO setLimitsToDto(ProblemDTO dto) {
		int time = 1000;
		int memory = 256;
		
		File propertyFile = getGradeProperties(dto.getId());
		if (propertyFile.exists()) {
       		try (InputStream is = new FileInputStream(propertyFile)) {
       			Properties props = new Properties();
	        	props.load(is);
	        			
	        	time = (int) (1000*Double.valueOf(props.getProperty("time", "1")) + 0.1);
	        	memory = Integer.valueOf(props.getProperty("memory", ""+memory));
	        } catch (Exception e) {
	        	log.error("Cannot read metadata for problem: " + dto.getId(), e);
	        }
		}
		
        dto.setTime(time);
        dto.setMemory(memory);
        return dto;
	}
	
	// @Scheduled(fixedDelay = 100*60*60*1000)
    public void populateCompetitionInfo() {
    	log.info("Starting job for populating competition info in problem.");
    	for (CompetitionProblem cp : competitionProblemRepository.findAll()) {
			Problem problem = cp.getProblem();
			if (problem.getYear() != null && problem.getCompetition() != null && problem.getGroup() != null) {
				continue;
			}
			if (problem.getYear() == null) {
				log.info("year is null");
			}
			if (problem.getCompetition() == null) {
				log.info("comp is null");
			}
			if (problem.getGroup() == null) {
				log.info("gruo is null");
			}
			log.info("Populating competition info for problem " + problem.toString());
			List<Competition> path = getPath(cp);
			for (Competition competition : path) {
				String name = competition.getLabel();
				name = name.trim();
				if (isYear(name)) {
					problem.setYear(Integer.parseInt(name));
				} else if (name.length() == 1) { // assume it's group
					String groupName = new HomographTranslator().translate(name);
					problem.setGroup(groupName);
				} else { // assume it's competition name
					problem.setCompetition(competition);
				}
			}
			problemRepository.save(problem);
		}
    }

	private List<Competition> getPath(CompetitionProblem cp) {
		List<Competition> path = new ArrayList<>();
		Competition competition = cp.getCompetition();
		
		while (competition != null && competition.getId() != 1) {
			path.add(competition);
			competition = competition.getParent();
		};
		return path;
	}
	
	private boolean isYear(String name) {
		if(name.length() != 4) return false;
		try {
			Integer.parseInt(name);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
}
