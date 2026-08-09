package com.olimpiici.arena.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeroturnaround.exec.ProcessExecutor;

import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.grader.WorkerPool;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.service.mapper.ProblemMapper;
import com.olimpiici.arena.service.mapper.TagMapper;
import com.olimpiici.arena.service.util.HomographTranslator;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * Service for managing Problem.
 * <p>
 * This service handles business logic for problems, including file operations
 * for problem packages, property management (limits), and tagging.
 */
@Service
@Transactional
public class ProblemService {

    private final Logger log = LoggerFactory.getLogger(ProblemService.class);

    private final Object lock = new Object();

    private final ProblemRepository problemRepository;

    private final ProblemMapper problemMapper;

    @Autowired
    private CompetitionProblemRepository competitionProblemRepository;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private SubmissionRepository submissionRepository;

    private final TagMapper tagMapper;

    private final TagService tagService;

	@Autowired
	private UserService userService;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private WorkerPool workerPool;

    public ProblemService(ProblemRepository problemRepository,
    		ProblemMapper problemMapper,
    		TagMapper tagMapper,
    		TagService tagService) {
        this.problemRepository = problemRepository;
        this.problemMapper = problemMapper;
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
     * <p>
     * This method will only delete the problem if it has no submissions
     * from regular users. Admin and author submissions are deleted first.
     *
     * @param id the id of the entity
     * @throws IOException if there's an error deleting problem files
     * @throws IllegalStateException if there are regular user submissions
     */
    public void delete(Long id) throws IOException {
        log.debug("Request to delete Problem : {}", id);

        List<CompetitionProblem> compProblems =
        		competitionProblemRepository.findByProblemId(id);

        List<Submission> submissions = compProblems.stream()
        	.flatMap( cp -> submissionRepository.findAllByCompetitionProblem(cp).stream())
        	.collect(Collectors.toList());

        boolean problemHasUserSubmissions = submissions.stream()
        	.anyMatch(s -> s.getUser().getId() != UserService.AUTHOR_ID && !userService.isUserAdmin(s.getUser()));
        if (problemHasUserSubmissions) {
        	throw new IllegalStateException("Cannot delete problem " +
        			id + " because there are user submissions");
        }

        submissions.forEach(s -> submissionService.delete(s.getId()));

        Problem problem = problemRepository.getOne(id);
        problem.setCanonicalCompetitionProblem(null);
        problemRepository.saveAndFlush(problem);

        competitionProblemRepository.deleteInBatch(compProblems);

        problemRepository.deleteById(id);

        File problemDir = getProblemFile(id, "");
        if (problemDir.exists()) {
        	FileUtils.deleteDirectory(problemDir);
        }

        workerPool.deleteProblem(id);
    }

    /**
     * Find tags for a specific problem.
     *
     * @param id the problem ID
     * @return list of tag DTOs
     */
    public List<TagDTO> findTags(Long id) {
    	Problem problem = problemRepository.getOne(id);
    	return findTags(problem.getTags());
    }

    /**
     * Find tags for a specific tag collection.
     *
     * @param tagCollection the collection to find tags for
     * @return list of tag DTOs
     */
    public List<TagDTO> findTags(TagCollection tagCollection) {
    	return tagService.findTagsForCollection(tagCollection)
	    	.map(tagMapper::toDto)
			.collect(Collectors.toList());
    }

    /**
     * Update tags for a problem.
     *
     * @param id the problem ID
     * @param newTags the list of new tags
     */
    public void updateTags(Long id, List<TagDTO> newTags) {
    	Problem problem = problemRepository.getOne(id);
    	TagCollection newCollection	=
    			tagService.updateTagsForCollection(problem.getTags(), newTags);

    	if (problem.getTags() == null) {
	    	problem.setTags(newCollection);
			problemRepository.save(problem);
    	}
    }

    /**
     * Get problem metadata from grade.properties file.
     *
     * @param problemId the problem ID
     * @return properties object containing limits and metadata
     */
	public Properties getProperties(Long problemId) {
    	Properties props = new Properties();
		File gradePropertiesFile = getGradeProperties(problemId);

		props.setProperty("time", "1");
		props.setProperty("memory", "256");

		if (!gradePropertiesFile.exists()) {
			return props;
		}

		synchronized (lock) {
			try (FileInputStream fis = new FileInputStream(gradePropertiesFile)) {
				props.load(fis);
			} catch (IOException e) {
				log.error("Cannot read metadata for problem: " + problemId, e);
			}
		}

		return props;
	}

    /**
     * Update the time limit for a problem.
     *
     * @param problemId the problem ID
     * @param newTimeLimitMs the new time limit in milliseconds
     * @throws Exception if there's an error updating the problem package
     */
	public void updateTimeLimit(Long problemId, int newTimeLimitMs) throws Exception {
		String timeValue = newTimeLimitMs/1000 + "." + newTimeLimitMs%1000;

		Properties props = getProperties(problemId);
		String oldTime = props.getProperty("time");
		if (oldTime != null && oldTime.equals(timeValue)) {
			return;
		}
		props.setProperty("time", timeValue);
		writeGradeProperties(problemId, props);
	}

    /**
     * Update the memory limit for a problem.
     *
     * @param problemId the problem ID
     * @param newMemoryLimitMb the new memory limit in megabytes
     * @throws Exception if there's an error updating the problem package
     */
	public void updateMemoryLimit(Long problemId, int newMemoryLimitMb) throws Exception {
		Properties props = getProperties(problemId);
		String newMemory = String.valueOf(newMemoryLimitMb);
		String oldMemory = props.getProperty("memory");
		if (oldMemory != null && oldMemory.equals(newMemory)) {
			return;
		}

		props.setProperty("memory", newMemory);
		writeGradeProperties(problemId, props);
	}

    /**
     * Get the solution file extension defined for a problem.
     *
     * @param problemId the problem ID
     * @return the extension string (e.g., "cpp")
     * @throws IOException if properties cannot be read
     */
	public String getSolutionFileExtension(long problemId) throws IOException {
		String defaultExtension = "cpp";
		Properties props = getProperties(problemId);
		return props.getProperty("extensions", defaultExtension);
	}

    /**
     * Internal helper to write properties back to the problem package.
     */
	private void writeGradeProperties(long problemId, Properties props) throws Exception {
		synchronized (lock) {
			unzipProblemZipLocked(problemId);
			File gradePropertiesFile = getGradeProperties(problemId);
			if (!gradePropertiesFile.exists()) {
				if (!gradePropertiesFile.getParentFile().exists()) {
					gradePropertiesFile.getParentFile().mkdirs();
				}
				gradePropertiesFile.createNewFile();
			}
			try (PrintWriter pw = new PrintWriter(gradePropertiesFile)) {
				props.store(pw, null);
			}
			recreateProblemZipLocked(problemId);
		}
	}

    /**
     * Helper to resolve paths within a problem's directory.
     */
	private File getProblemFile(long problemId, String filename) {
		return Paths.get(applicationProperties.getWorkDir(), "problems", ""+problemId, filename)
				.toFile();
	}

	private File getProblemZip(long problemId) {
		return getProblemFile(problemId, "problem.zip");
	}

	private File getUnzippedProblemFolder(long problemId) {
		return getProblemFile(problemId, "problem");
	}

	private File getGradeProperties(long problemId) {
		return getProblemFile(problemId, Paths.get("problem", "grade.properties").toString());
	}

    /**
     * Unzips the problem package zip file.
     */
	public void unzipProblemZip(long problemId) throws ZipException, IOException {
		synchronized(lock) {
			unzipProblemZipLocked(problemId);
		}
	}

	private void unzipProblemZipLocked(long problemId) throws ZipException, IOException {
		File zipFile = getProblemZip(problemId);
        ZipFile zipZipFile = new ZipFile(zipFile);

        File zipDir = getUnzippedProblemFolder(problemId);
        if (zipDir.exists()) {
            FileUtils.deleteDirectory(zipDir);
        }
        zipDir.mkdirs();
        zipZipFile.extractAll(zipDir.getAbsolutePath());
	}

    /**
     * Recreates the problem zip file from the unzipped directory.
     */
	private void recreateProblemZipLocked(long problemId) throws Exception {
        File problemsDir = getUnzippedProblemFolder(problemId);
		ProcessExecutor executor = new ProcessExecutor()
      			.command("zip", "-r", "problem.zip", ".")
      			.directory(problemsDir);
      	executor.execute();
      	File problemZipNew = getProblemFile(problemId, Paths.get("problem", "problem.zip").toString());
      	File problemZipOrig = getProblemZip(problemId);
      	problemZipOrig.delete();
      	problemZipNew.renameTo(problemZipOrig);

      	workerPool.deleteProblem(problemId);
	}

    /**
     * Populates time and memory limits into a ProblemDTO from grade.properties.
     */
	public ProblemDTO setLimitsToDto(ProblemDTO dto) {
		Properties props = getProperties(dto.getId());

		int time = (int) (1000*Double.valueOf(props.getProperty("time", "")) + 0.1);
		dto.setTime(time);

		int memory = Integer.valueOf(props.getProperty("memory", ""));
        dto.setMemory(memory);

		dto.setExtension(props.getProperty("extensions", "cpp"));
        return dto;
	}

    /**
     * Scheduled task to infer and populate competition metadata for problems.
     * <p>
     * Runs daily. It traverses the competition tree to determine the year,
     * competition, and group for each problem and updates the database.
     */
	@Scheduled(fixedDelay = 24*60*60*1000) // Every day
    public void populateCompetitionInfo() {
    	log.info("Starting job for populating competition info in problem.");
    	for (CompetitionProblem cp : competitionProblemRepository.findAll()) {
    		List<Competition> path = getPath(cp);
    		// If CP is not connected to root, skip it
    		if (path.isEmpty() || path.get(path.size() - 1).getId() != 1) {
    			continue;
    		}

			Integer year = null;
			Competition competition = null;
			String groupName = null;
			for (Competition c : path) {
				String name = c.getLabel();
				name = name.trim();
				if (year == null && isYear(name)) {
					year = Integer.parseInt(name);
				} else if (groupName == null && name.length() == 1) { // assume it's group
					groupName = new HomographTranslator().translate(name);
				} else if (competition == null && name.length() > 1) { // assume it's competition name
					competition = c;
				}
			}

			Problem problem = cp.getProblem();
			if (Objects.equals(problem.getYear(), year)
                    && problem.getCompetition() != null
                    && problem.getCompetition().getId() == competition.getId()
                    && Objects.equals(problem.getGroup(), groupName)
                    && problem.getCanonicalCompetitionProblem() != null
                    && problem.getCanonicalCompetitionProblem().getId() == cp.getId()) {
				continue;
			}

			problem.setYear(year);
			problem.setCompetition(competition);
			problem.setGroup(groupName);
			problem.setCanonicalCompetitionProblem(cp);
			log.info("Populating competition info for problem " + problem.toString());
			problemRepository.save(problem);
		}
    }

    /**
     * Returns the breadcrumb path of competitions for a given competition problem.
     */
	private List<Competition> getPath(CompetitionProblem cp) {
		List<Competition> path = new ArrayList<>();
		Competition competition = cp.getCompetition();

		while (competition != null) {
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

    /**
     * Gets the directory where problem files are stored.
     */
	public File getProblemDir(String workDir, Long taskId) {
		return new File(workDir + "/problems/" + taskId + "/problem");
	}

    /**
     * Identifies the PDF description file for a task.
     */
	public Optional<File> getTaskDescription(String workDir, Long taskId) {
		try {
			File dir = getProblemDir(workDir, taskId);
			return Files.walk(dir.toPath())
					.filter(Files::isRegularFile)
					.map(Path::toFile)
					.filter(f -> f.getName().toLowerCase().endsWith(".pdf"))
					.sorted((f1, f2) -> Integer.compare(f1.getAbsolutePath().length(), f2.getAbsolutePath().length()))
					.findFirst();
		} catch (Exception e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

    /**
     * Identifies the author's solution file for a task by looking for specific
     * filenames (e.g., author.cpp, title.cpp, or containing "100").
     */
	public Optional<File> getAuthorSolution(String workDir, Long taskId) {
		try {
			File dir = getProblemDir(workDir, taskId);
			String ext = getSolutionFileExtension(taskId);
			List<File> potentialSols = Files.walk(dir.toPath())
					.filter(Files::isRegularFile)
					.map(Path::toFile)
					.filter(f -> f.getName().toLowerCase().endsWith("." + ext))
					.collect(Collectors.toList());

			if (potentialSols.isEmpty()) {
				return Optional.empty();
			}

			if (potentialSols.size() == 1) {
				return Optional.of(potentialSols.get(0));
			}

			Optional<File> file = potentialSols.stream()
					.filter(f -> f.getName().toLowerCase().equals("author." + ext))
					.findFirst();
			if (file.isPresent()) {
				return file;
			}

			Optional<String> title = problemRepository.findById(taskId)
					.map(problem -> problem.getTitle())
					.map(t -> t.toLowerCase() + "." + ext);
			if (!title.isPresent()) {
				return  Optional.empty();
			}

			file = potentialSols.stream()
					.filter(f -> f.getName().toLowerCase().equals(title.get()))
					.findFirst();
			if (file.isPresent()) {
				return file;
			}

			return potentialSols.stream()
					.filter(f -> f.getName().toLowerCase().contains("100"))
					.findFirst();

		} catch (Exception e) {
			e.printStackTrace();
			return Optional.empty();
		}
	}

}
