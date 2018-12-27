package com.olimpiici.arena.web.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;
import com.olimpiici.arena.config.ApplicationProperties;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.service.dto.SubmissionDTO;

import io.github.jhipster.web.util.ResponseUtil;

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

}