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
import com.olimpiici.arena.domain.TagCollection;
import com.olimpiici.arena.grader.WorkerPool;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.ProblemRepository;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.service.mapper.ProblemMapper;
import com.olimpiici.arena.service.mapper.TagMapper;
import com.olimpiici.arena.service.util.HomographTranslator;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * Service for managing Problem.
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

    private final TagMapper tagMapper;

    private final TagService tagService;

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

	public String getSolutionFileExtension(long problemId) throws IOException {
		String defaultExtension = "cpp";
		Properties props = getProperties(problemId);
		return props.getProperty("extensions", defaultExtension);
	}

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

	public ProblemDTO setLimitsToDto(ProblemDTO dto) {
		Properties props = getProperties(dto.getId());

		int time = (int) (1000*Double.valueOf(props.getProperty("time", "")) + 0.1);
		dto.setTime(time);

		int memory = Integer.valueOf(props.getProperty("memory", ""));
        dto.setMemory(memory);
        return dto;
	}

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

	public File getProblemDir(String workDir, Long taskId) {
		return new File(workDir + "/problems/" + taskId + "/problem");
	}

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
