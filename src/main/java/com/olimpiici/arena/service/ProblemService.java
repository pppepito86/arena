package com.olimpiici.arena.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
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
import com.olimpiici.arena.service.dto.ProblemPackageInfoDTO;
import com.olimpiici.arena.service.dto.TagDTO;
import com.olimpiici.arena.service.mapper.ProblemMapper;
import com.olimpiici.arena.service.mapper.TagMapper;
import com.olimpiici.arena.service.util.HomographTranslator;
import com.olimpiici.arena.service.util.ProblemPackageInspector;
import com.olimpiici.arena.service.util.ProblemPackageResolver;
import com.olimpiici.arena.service.util.ProblemZipUtils;

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

    @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return problemRepository.existsById(id);
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

    static final int DEFAULT_TIME_MS = 1000;
    static final int DEFAULT_MEMORY_MB = 256;
    static final String DEFAULT_EXTENSION = "cpp";

    /**
     * Get problem metadata from grade.properties file, preferring the authoritative
     * {@code problem.zip} (which is what the worker grades) and falling back to the
     * unzipped problem folder if {@code problem.zip} does not exist or has no properties file.
     *
     * @param problemId the problem ID
     * @return properties object containing limits and metadata
     */
	public Properties getProperties(Long problemId) {
    	Properties props = new Properties();
		props.setProperty("time", "1");
		props.setProperty("memory", "256");

		synchronized (lock) {
			try {
				File zipFile = getProblemZip(problemId);
				if (zipFile.isFile()) {
					try (ProblemZipUtils.OpenedZip opened = ProblemZipUtils.openZipFile(zipFile)) {
						Optional<String> zipEntry = ProblemPackageResolver.findPropertiesEntry(
							ProblemZipUtils.listFileEntries(opened));
						if (zipEntry.isPresent()) {
							Optional<byte[]> bytes = ProblemZipUtils.readEntry(opened, zipEntry.get());
							if (bytes.isPresent()) {
								try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes.get())) {
									props.load(bais);
									return props;
								}
							}
						}
					} catch (IOException e) {
						log.warn("Cannot read metadata from problem.zip for problem {}, falling back to unzipped dir", problemId, e);
					}
				}

				File unzippedFolder = getUnzippedProblemFolder(problemId);
				Optional<String> onDiskEntry = ProblemPackageResolver.findPropertiesEntry(
					ProblemPackageResolver.listRelativeFiles(unzippedFolder));
				if (onDiskEntry.isPresent()) {
					File propsFile = new File(unzippedFolder, onDiskEntry.get());
					try (FileInputStream fis = new FileInputStream(propsFile)) {
						props.load(fis);
					}
				}
			} catch (IOException e) {
				log.error("Cannot read metadata for problem: " + problemId, e);
			}
		}

		return props;
	}

    /**
     * Format a time limit in milliseconds as a decimal seconds string using
     * {@link Locale#ROOT} (e.g., 1000 -> "1", 1050 -> "1.05", 250 -> "0.25").
     */
    static String formatTimeLimitSeconds(int timeLimitMs) {
        return BigDecimal.valueOf(timeLimitMs, 3).stripTrailingZeros().toPlainString();
    }

    public static Optional<Integer> parseTimeMillis(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            double seconds = Double.parseDouble(trimmed);
            if (!Double.isFinite(seconds) || seconds <= 0) {
                return Optional.empty();
            }
            return Optional.of((int) Math.round(seconds * 1000.0));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<Integer> parseMemoryMb(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        try {
            int memory = Integer.parseInt(trimmed);
            if (memory <= 0) {
                return Optional.empty();
            }
            return Optional.of(memory);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static List<String> parseAllowedExtensions(String raw) {
        if (raw == null) {
            return Collections.singletonList(DEFAULT_EXTENSION);
        }
        List<String> extensions = Arrays.stream(raw.split(","))
            .map(String::trim)
            .map(s -> s.toLowerCase(Locale.ROOT))
            .filter(s -> !s.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        return extensions.isEmpty() ? Collections.singletonList(DEFAULT_EXTENSION) : extensions;
    }

    static String normalizePrimaryExtension(String raw) {
        return parseAllowedExtensions(raw).get(0);
    }

    /**
     * Atomically updates the time and/or memory limit in {@code grade.properties}
     * in a single ZIP stream pass and evicts the worker cache at most once.
     *
     * @param problemId the problem ID
     * @param newTimeLimitMs new time limit in milliseconds, or {@code null} to leave unchanged
     * @param newMemoryLimitMb new memory limit in megabytes, or {@code null} to leave unchanged
     * @throws Exception if there's an error updating the problem package
     */
	public void updateLimits(Long problemId, Integer newTimeLimitMs, Integer newMemoryLimitMb) throws Exception {
		if (newTimeLimitMs == null && newMemoryLimitMb == null) {
			return;
		}
		if (newTimeLimitMs != null && newTimeLimitMs <= 0) {
			throw new IllegalArgumentException("Time limit must be positive, got: " + newTimeLimitMs);
		}
		if (newMemoryLimitMb != null && newMemoryLimitMb <= 0) {
			throw new IllegalArgumentException("Memory limit must be positive, got: " + newMemoryLimitMb);
		}
		String timeValue = newTimeLimitMs != null ? formatTimeLimitSeconds(newTimeLimitMs) : null;
		String memoryValue = newMemoryLimitMb != null ? String.valueOf(newMemoryLimitMb) : null;
		modifyGradeProperties(problemId, props -> {
			boolean changed = false;
			if (newTimeLimitMs != null) {
				String rawTime = props.getProperty("time");
				Optional<Integer> oldTimeMs = parseTimeMillis(rawTime);
				boolean hasExtraWhitespace = rawTime != null && !rawTime.equals(rawTime.trim());
				if (hasExtraWhitespace || !oldTimeMs.isPresent() || oldTimeMs.get() != newTimeLimitMs.intValue()) {
					props.setProperty("time", timeValue);
					changed = true;
				}
			}
			if (newMemoryLimitMb != null) {
				String rawMemory = props.getProperty("memory");
				Optional<Integer> oldMemoryMb = parseMemoryMb(rawMemory);
				boolean hasExtraWhitespace = rawMemory != null && !rawMemory.equals(rawMemory.trim());
				if (hasExtraWhitespace || !oldMemoryMb.isPresent() || oldMemoryMb.get() != newMemoryLimitMb.intValue()) {
					props.setProperty("memory", memoryValue);
					changed = true;
				}
			}
			return changed;
		});
	}

    /**
     * Update the time limit for a problem.
     *
     * @param problemId the problem ID
     * @param newTimeLimitMs the new time limit in milliseconds
     * @throws Exception if there's an error updating the problem package
     */
	public void updateTimeLimit(Long problemId, int newTimeLimitMs) throws Exception {
		updateLimits(problemId, newTimeLimitMs, null);
	}

    /**
     * Update the memory limit for a problem.
     *
     * @param problemId the problem ID
     * @param newMemoryLimitMb the new memory limit in megabytes
     * @throws Exception if there's an error updating the problem package
     */
	public void updateMemoryLimit(Long problemId, int newMemoryLimitMb) throws Exception {
		updateLimits(problemId, null, newMemoryLimitMb);
	}

    /**
     * Get the solution file extension defined for a problem.
     *
     * @param problemId the problem ID
     * @return the extension string (e.g., "cpp")
     * @throws IOException if properties cannot be read
     */
	public String getSolutionFileExtension(long problemId) throws IOException {
		Properties props = getProperties(problemId);
		return normalizePrimaryExtension(props.getProperty("extensions"));
	}

    /**
     * Atomically reads, mutates, and persists grade.properties in place inside problem.zip
     * (preserving its existing path such as {@code A1_belote/grade.properties} or
     * {@code tests/grade.properties} so we never create a duplicate root grade.properties)
     * and syncs the unzipped on-disk copy if its parent directory exists.
     */
	private void modifyGradeProperties(long problemId, Function<Properties, Boolean> mutator) throws Exception {
		boolean modified = false;
		synchronized (lock) {
			File zipFile = getProblemZip(problemId);
			File unzippedFolder = getUnzippedProblemFolder(problemId);

			Optional<String> existingZipEntry = Optional.empty();
			Optional<byte[]> existingZipBytes = Optional.empty();
			if (zipFile.isFile()) {
				try (ProblemZipUtils.OpenedZip opened = ProblemZipUtils.openZipFile(zipFile)) {
					existingZipEntry = ProblemPackageResolver.findPropertiesEntry(
						ProblemZipUtils.listFileEntries(opened));
					if (existingZipEntry.isPresent()) {
						existingZipBytes = ProblemZipUtils.readEntry(opened, existingZipEntry.get());
					}
				}
			}
			Optional<String> existingDiskEntry = unzippedFolder.isDirectory()
				? ProblemPackageResolver.findPropertiesEntry(ProblemPackageResolver.listRelativeFiles(unzippedFolder))
				: Optional.empty();

			String entryPath = existingZipEntry.orElseGet(() -> existingDiskEntry.orElse("grade.properties"));

			Properties props = new Properties();
			props.setProperty("time", "1");
			props.setProperty("memory", "256");

			File unzippedPropsFile = new File(unzippedFolder, ProblemPackageResolver.normalizeRelativePath(entryPath));
			if (existingZipBytes.isPresent()) {
				try (ByteArrayInputStream bais = new ByteArrayInputStream(existingZipBytes.get())) {
					props.load(bais);
				}
			} else if (unzippedPropsFile.isFile()) {
				try (FileInputStream fis = new FileInputStream(unzippedPropsFile)) {
					props.load(fis);
				}
			}

			if (!Boolean.TRUE.equals(mutator.apply(props))) {
				return;
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			props.store(baos, null);
			byte[] updatedBytes = baos.toByteArray();

			if (zipFile.isFile()) {
				ProblemZipUtils.updateOrAddEntry(zipFile, entryPath, updatedBytes);
			}
			Path propsPath = unzippedPropsFile.toPath();
			Path parent = propsPath.getParent();
			if (!zipFile.isFile() && parent != null) {
				Files.createDirectories(parent);
			}
			if (!zipFile.isFile() || (parent != null && Files.isDirectory(parent))) {
				Files.write(propsPath, updatedBytes);
			}
			modified = true;
		}

		if (modified) {
			workerPool.deleteProblem(problemId);
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

    /**
     * Stages an uploaded problem ZIP, validates & extracts it safely into a sibling temporary
     * directory, and atomically swaps both problem.zip and problem/ under lock.
     */
	public void replaceProblemZip(long problemId, InputStream zipStream) throws IOException {
		File problemRoot = getProblemFile(problemId, "");
		Files.createDirectories(problemRoot.toPath());

		String suffix = UUID.randomUUID().toString();
		File tempZip = new File(problemRoot, "problem.zip.tmp." + suffix);
		File tempDir = new File(problemRoot, "problem.tmp." + suffix);
		try {
			FileUtils.copyInputStreamToFile(zipStream, tempZip);
			ProblemZipUtils.extractSafely(tempZip, tempDir);

			synchronized (lock) {
				ProblemZipUtils.moveAtomically(tempZip.toPath(), getProblemZip(problemId).toPath());
				swapDirectoryLocked(tempDir, getUnzippedProblemFolder(problemId), suffix);
			}
		} catch (IOException | RuntimeException e) {
			FileUtils.deleteQuietly(tempZip);
			FileUtils.deleteQuietly(tempDir);
			throw e;
		}
	}

    /**
     * Unzips the problem package zip file safely into the unzipped problem folder.
     */
	public void unzipProblemZip(long problemId) throws IOException {
		synchronized(lock) {
			unzipProblemZipLocked(problemId);
		}
	}

	private void unzipProblemZipLocked(long problemId) throws IOException {
		File zipFile = getProblemZip(problemId);
		File problemRoot = zipFile.getParentFile();
		String suffix = UUID.randomUUID().toString();
		File tempDir = new File(problemRoot, "problem.tmp." + suffix);
		try {
			ProblemZipUtils.extractSafely(zipFile, tempDir);
			swapDirectoryLocked(tempDir, getUnzippedProblemFolder(problemId), suffix);
		} catch (IOException | RuntimeException e) {
			FileUtils.deleteQuietly(tempDir);
			throw e;
		}
	}

	private void swapDirectoryLocked(File tempDir, File targetDir, String suffix) throws IOException {
		File backupDir = new File(targetDir.getParentFile(), targetDir.getName() + ".old." + suffix);
		boolean movedOld = false;
		if (targetDir.exists()) {
			ProblemZipUtils.moveAtomically(targetDir.toPath(), backupDir.toPath());
			movedOld = true;
		}
		try {
			ProblemZipUtils.moveAtomically(tempDir.toPath(), targetDir.toPath());
		} catch (IOException | RuntimeException e) {
			if (movedOld && !targetDir.exists() && backupDir.exists()) {
				try {
					ProblemZipUtils.moveAtomically(backupDir.toPath(), targetDir.toPath());
				} catch (IOException restoreEx) {
					log.error("Failed to restore previous problem directory {}", targetDir, restoreEx);
				}
			}
			throw e;
		}
		if (movedOld) {
			FileUtils.deleteQuietly(backupDir);
		}
	}

    /**
     * Populates time and memory limits into a ProblemDTO from grade.properties.
     */
	public ProblemDTO setLimitsToDto(ProblemDTO dto) {
		Properties props = getProperties(dto.getId());

		String rawTime = props.getProperty("time");
		Optional<Integer> parsedTime = parseTimeMillis(rawTime);
		if (!parsedTime.isPresent()) {
			log.warn("Invalid time limit '{}' for problem {}, falling back to {}ms", rawTime, dto.getId(), DEFAULT_TIME_MS);
		}
		dto.setTime(parsedTime.orElse(DEFAULT_TIME_MS));

		String rawMemory = props.getProperty("memory");
		Optional<Integer> parsedMemory = parseMemoryMb(rawMemory);
		if (!parsedMemory.isPresent()) {
			log.warn("Invalid memory limit '{}' for problem {}, falling back to {}MB", rawMemory, dto.getId(), DEFAULT_MEMORY_MB);
		}
        dto.setMemory(parsedMemory.orElse(DEFAULT_MEMORY_MB));

		dto.setExtension(normalizePrimaryExtension(props.getProperty("extensions")));
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
    	log.debug("Starting job for populating competition info in problem.");
    	Map<Long, CompetitionInfo> targetInfoByProblemId = new LinkedHashMap<>();
    	List<CompetitionProblem> allCompetitionProblems = new ArrayList<>(competitionProblemRepository.findAll());
    	allCompetitionProblems.sort(Comparator.comparing(CompetitionProblem::getId, Comparator.nullsFirst(Comparator.naturalOrder())));
    	for (CompetitionProblem cp : allCompetitionProblems) {
    		List<Competition> path = getPath(cp);
    		// If CP is not connected to root, skip it
    		if (path.isEmpty() || !Long.valueOf(1L).equals(path.get(path.size() - 1).getId())) {
    			continue;
    		}

			Integer year = null;
			Competition competition = null;
			String groupName = null;
			for (Competition c : path) {
				String name = c.getLabel();
				if (name == null) {
					continue;
				}
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
			if (problem != null && problem.getId() != null) {
				targetInfoByProblemId.put(problem.getId(), new CompetitionInfo(problem, year, competition, groupName, cp));
			}
		}

    	int updatedCount = 0;
    	for (CompetitionInfo info : targetInfoByProblemId.values()) {
    		Problem problem = info.problem;
    		Long currentCompetitionId = problem.getCompetition() != null ? problem.getCompetition().getId() : null;
    		Long targetCompetitionId = info.competition != null ? info.competition.getId() : null;
    		Long currentCanonicalCpId = problem.getCanonicalCompetitionProblem() != null
    				? problem.getCanonicalCompetitionProblem().getId() : null;
    		Long targetCanonicalCpId = info.canonicalCp != null ? info.canonicalCp.getId() : null;

			if (Objects.equals(problem.getYear(), info.year)
                    && Objects.equals(currentCompetitionId, targetCompetitionId)
                    && Objects.equals(problem.getGroup(), info.groupName)
                    && Objects.equals(currentCanonicalCpId, targetCanonicalCpId)) {
				continue;
			}

			problem.setYear(info.year);
			problem.setCompetition(info.competition);
			problem.setGroup(info.groupName);
			problem.setCanonicalCompetitionProblem(info.canonicalCp);
			log.debug("Populating competition info for problem {}", problem);
			problemRepository.save(problem);
			updatedCount++;
    	}
    	if (updatedCount > 0) {
    		log.info("Populated competition info for {} problems.", updatedCount);
    	}
    }

    private static final class CompetitionInfo {
    	final Problem problem;
    	final Integer year;
    	final Competition competition;
    	final String groupName;
    	final CompetitionProblem canonicalCp;

    	CompetitionInfo(Problem problem, Integer year, Competition competition, String groupName, CompetitionProblem canonicalCp) {
    		this.problem = problem;
    		this.year = year;
    		this.competition = competition;
    		this.groupName = groupName;
    		this.canonicalCp = canonicalCp;
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
     * Identifies the PDF statement file for a task, prioritizing Bulgarian statements
     * and excluding analyses, editorials, figures, and macOS metadata files.
     */
	public Optional<File> getTaskDescription(String workDir, Long taskId) {
		synchronized (lock) {
			File dir = getProblemDir(workDir, taskId);
			if (!dir.isDirectory()) {
				log.error("Problem directory does not exist for task {}: {}", taskId, dir);
				return Optional.empty();
			}
			try {
				List<String> relativeFiles = ProblemPackageResolver.listRelativeFiles(dir);
				return ProblemPackageResolver.findStatementPdf(relativeFiles)
						.map(rel -> new File(dir, rel));
			} catch (Exception e) {
				log.warn("Failed to find task description for problem {} in {}", taskId, dir, e);
				return Optional.empty();
			}
		}
	}

    /**
     * Identifies the author's full solution file for a task, excluding contestant
     * stubs, checkers, graders, partial/wrong/cheat solutions, and macOS metadata.
     */
	public Optional<File> getAuthorSolution(String workDir, Long taskId) {
		String title = problemRepository.findById(taskId)
				.map(Problem::getTitle)
				.orElse(null);
		synchronized (lock) {
			File dir = getProblemDir(workDir, taskId);
			if (!dir.isDirectory()) {
				log.error("Problem directory does not exist for task {}: {}", taskId, dir);
				return Optional.empty();
			}
			try {
				Properties props = getProperties(taskId);
				List<String> allowedExtensions = parseAllowedExtensions(props.getProperty("extensions"));
				List<String> relativeFiles = ProblemPackageResolver.listRelativeFiles(dir);
				return ProblemPackageResolver.findAuthorSolution(relativeFiles, allowedExtensions, title)
						.map(rel -> new File(dir, rel));
			} catch (Exception e) {
				log.warn("Failed to find author solution for problem {} in {}", taskId, dir, e);
				return Optional.empty();
			}
		}
	}

    /**
     * Inspects a problem's uploaded package (problem.zip and unzipped problem/ folder)
     * and returns a diagnostic summary for admins.
     */
	public ProblemPackageInfoDTO inspectPackage(Long problemId) throws IOException {
		String title = problemRepository.findById(problemId)
				.map(Problem::getTitle)
				.orElse(null);
		synchronized (lock) {
			return ProblemPackageInspector.inspect(
					getProblemZip(problemId),
					getUnzippedProblemFolder(problemId),
					title);
		}
	}

}
