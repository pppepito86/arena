package com.olimpiici.arena.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.service.AlertingService;

/**
 * REST controller for managing Problem.
 */
@RestController
@RequestMapping("/management/debug")
public class DebugResource {

    private final Logger log = LoggerFactory.getLogger(DebugResource.class);

    @Autowired
    private AlertingService alertingService;


    @PostMapping("/send-mail")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public void sendMail(){
        alertingService.sendAlert("Test mail");
    }

}
