package com.olimpiici.arena.web.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Optional;

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
    
    /**
     * GET  /problems/:id/pdf : get the problem description in pdf format.
     *
     * @param id the id of the problem to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the problemDTO, or with status 404 (Not Found)
     */
    @GetMapping("/problems/{id}/pdf")
    @Timed
    public ResponseEntity<?> getProblemPdf(@PathVariable Long id, 
    		@RequestParam(value = "download", defaultValue = "false") Boolean download) throws Exception {
        log.debug("REST request to get Problem PDF: {}", id);
    	
    	File dir = new File(applicationProperties.getWorkDir() + "/problems/" + id + "/problem");
    	log.debug("***dir is " + dir.getAbsolutePath());
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

}