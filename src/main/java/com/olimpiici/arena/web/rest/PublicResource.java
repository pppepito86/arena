package com.olimpiici.arena.web.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.pesho.grader.SubmissionScore;
import org.pesho.grader.step.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.service.CompetitionProblemService;
import com.olimpiici.arena.service.ProblemService;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.service.dto.CompetitionProblemDTO;
import com.olimpiici.arena.service.dto.ProblemDTO;
import com.olimpiici.arena.service.dto.SubmissionDTO;

import io.github.jhipster.web.util.ResponseUtil;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

/**
 * REST controller for managing Problem.
 */
@RestController
@RequestMapping("/api/public")
public class PublicResource {

    private final Logger log = LoggerFactory.getLogger(PublicResource.class);

    @Autowired
    private ApplicationProperties applicationProperties;
    
    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private ProblemService problemService;

    @Autowired
    private CompetitionProblemService competitionProblemService;
    /**
     * GET  /problems/:id/pdf : get the problem description in pdf format.
     *
     * @param id the id of the problem to retrieve
     * @return the ResponseEntity with status 200 (OK) and the pdf, or with status 404 (Not Found)
     */
    @GetMapping("/problems/{id}/pdf")
    @Timed
    public ResponseEntity<?> getProblemPdf(@PathVariable Long id, 
    		@RequestParam(value = "download", defaultValue = "false") Boolean download) throws Exception {
        log.debug("REST request to get Problem PDF: {}", id);
    	
    	File dir = new File(applicationProperties.getWorkDir() + "/problems/" + id + "/problem");
    	Optional<File> pdf = Arrays.stream(dir.listFiles()).filter(f -> f.getName().endsWith(".pdf")).findAny();
    	Optional<InputStreamResource> isr = pdf.map(f -> {
			try {
				return new FileInputStream(f);
			} catch (FileNotFoundException e) {
				return null;
			}
		}).map(fis -> new InputStreamResource(fis));
    	
    	HttpHeaders respHeaders = new HttpHeaders();
    	if (download) {
    		respHeaders.setContentDispositionFormData("attachment", pdf.map(f -> f.getName()).orElse("problem.pdf"));
    	} else {
    		respHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
    	}
    	
    	return ResponseUtil.wrapOrNotFound(isr, respHeaders);
    }

    /**
     * GET  /problems/:id/zip : get the problem archive in zip format.
     *
     * @param id the id of the problem to retrieve
     * @return the ResponseEntity with status 200 (OK), downloading the zip, or with status 404 (Not Found)
     */
    @GetMapping("/problems/{id}/zip")
    @Timed
    public ResponseEntity<?> getProblemZip(@PathVariable Long id) throws Exception {
        log.debug("REST request to get Problem ZIP: {}", id);
    	
        File zipFile = Paths.get(applicationProperties.getWorkDir(), "problems", ""+id, "problem.zip").toFile();
        InputStreamResource isr = null;
        if (zipFile.exists()) {
        	isr = new InputStreamResource(new FileInputStream(zipFile));
        }
    	
    	HttpHeaders respHeaders = new HttpHeaders();
    	respHeaders.setContentDispositionFormData("attachment", "problem.zip");
    	
    	return ResponseUtil.wrapOrNotFound(Optional.of(isr), respHeaders);
    }

    @GetMapping("/time_limits")
    @Timed
    public ResponseEntity<?> setTimeLimits() throws Exception {
        log.debug("REST request to get set time limits");
    	
        
        File problemsDir = Paths.get(applicationProperties.getWorkDir(), "problems").toFile();
        for (File f: problemsDir.listFiles()) {
        	File author = Paths.get(f.getAbsolutePath(), "problem", "author", "author.cpp").toFile();
        	if (!author.exists()) continue;
        	
        	long id = Integer.valueOf(f.getName());
        	SubmissionDTO submission = new SubmissionDTO();
        	submission.setCompetitionProblemId(id);
        	submission.setUserId(4L);

        	for (int i = 0; i < 3; i++) {
        		SubmissionDTO s = submissionService.save(submission);        	
        		File submissionFile = Paths.get(applicationProperties.getWorkDir(), "submissions", ""+s.getId(), "solution.cpp").toFile();
        		FileUtils.copyFile(author, submissionFile);
        		s.setVerdict("waiting");
        		submissionService.save(s);
        	}
        }
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/copy_submissions")
    @Timed
    public ResponseEntity<?> copySubmissions() throws Exception {
        log.debug("REST request to get set time limits");
    	
        List<SubmissionDTO> byVerdict = submissionService.findSubmissionByVerdict("CE");
        for (SubmissionDTO s: byVerdict) {
        	long problemId = competitionProblemService.findOne(s.getCompetitionProblemId()).get().getProblemId();

        	File author = Paths.get(applicationProperties.getWorkDir(), "problems", ""+problemId, "problem", "author", "author.cpp").toFile();
    		File submissionFile = Paths.get(applicationProperties.getWorkDir(), "submissions", ""+s.getId(), "solution.cpp").toFile();
    		FileUtils.copyFile(author, submissionFile);
        }
        
        return ResponseEntity.noContent().build();
    }

	private ObjectMapper mapper = new ObjectMapper();
    
    @GetMapping("/fix_verdicts")
    @Timed
    public ResponseEntity<?> fixVerdicts() throws Exception {
        log.debug("REST request to get set time limits");
    	
        List<SubmissionDTO> byVerdict = submissionService.findSubmissionByVerdict("CE");
        for (SubmissionDTO s: byVerdict) {
        	String details = s.getDetails();
        	try {
        		SubmissionScore score = mapper.readValue(details, SubmissionScore.class);
        		String result = "";
    			StepResult[] values = score.getScoreSteps().values().toArray(new StepResult[0]);
    			if (values.length > 1) {
    				for (int i = 1; i < values.length; i++) {
    					StepResult step = values[i];
    					if (i != 1)
    						result += ",";
    					result += step.getVerdict();
    				}
    			} else {
    				result = values[0].getVerdict().toString();
    			}
    			s.setVerdict(result);
    			submissionService.save(s);
        	} catch (Exception e) {
			}
        }
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/times")
    @Timed
    public ResponseEntity<?> setTimes(
    		@RequestParam(value = "set", defaultValue = "false") Boolean set) throws Exception {
        log.debug("REST request to get set time limits");
        
        PageRequest page = PageRequest.of(0, 10000);
        List<CompetitionProblemDTO> competitionProblems = competitionProblemService.findAll(page).getContent();
        log.debug("competition problems: " + competitionProblems.size());
        
        for (CompetitionProblemDTO competitionProblem: competitionProblems) {
        	
        	List<SubmissionDTO> submissions = submissionService.findSubmissionsByCompetitionProblem(competitionProblem.getId(), page).getContent();
        	log.debug("competition problem " + competitionProblem.getId() + " has " + submissions.size() + " submissions");
        	
        	ProblemDTO problem = problemService.findOne(competitionProblem.getProblemId()).get();
        	if (submissions.size() == 3 && submissions.stream().mapToInt(s -> s.getPoints()).allMatch(p -> p == 100)) {
        		List<Integer> times = submissions.stream().map(s -> s.getTimeInMillis()).collect(Collectors.toList());
        		int max = times.stream().mapToInt(t -> t).max().getAsInt();
        		
        		int limit = (max*15/10)/100+1;

        		log.debug("limit for problem<"+problem.getId()+"> with times "+times+" will be "+limit/10+"."+limit%10);
        		
        		if (set) {
        			String timeProp = "time = "+limit/10+"."+limit%10;
                	File propeties = Paths.get(applicationProperties.getWorkDir(), "problems", ""+problem.getId(), "problem", "grade.properties").toFile();
                	FileUtils.writeStringToFile(propeties, timeProp, StandardCharsets.UTF_8);
                	
                	ZipFile zip = new ZipFile(Paths.get(applicationProperties.getWorkDir(), "problems", ""+problem.getId(), "problem.zip").toFile());
                	
                	ZipParameters parameters = new ZipParameters();
                	parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
                	parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
                	zip.addFile(propeties, parameters);
        		}
        		
        	} else {
        		log.debug("competition problem " + competitionProblem.getId() + " version 0");
            	
        		if (!set) continue;
        		
        		problem.setVersion(0);
        		problemService.save(problem);
        	}
        	
        }
        
        return ResponseEntity.noContent().build();
    }
}