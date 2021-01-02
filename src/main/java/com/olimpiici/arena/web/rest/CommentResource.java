package com.olimpiici.arena.web.rest;

import java.net.URISyntaxException;
import java.util.List;

import com.codahale.metrics.annotation.Timed;
import com.olimpiici.arena.domain.Comment;
import com.olimpiici.arena.domain.Topic;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.security.AuthoritiesConstants;
import com.olimpiici.arena.security.SecurityUtils;
import com.olimpiici.arena.service.CommentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Tag.
 */
@RestController
@RequestMapping("/api")
public class CommentResource {

    private final Logger log = LoggerFactory.getLogger(TagResource.class);

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.USER + "\")")
    @PostMapping("/topic/{id}/comment")
    @Timed
    public ResponseEntity<Object> postComment(@PathVariable Long id, @RequestBody String content)
            throws URISyntaxException {
        User user = userRepository
                .findOneByLogin(SecurityUtils.getCurrentUserLogin().get()).get();
        commentService.postCommentToTopic(id, user, content);        
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.USER + "\")")
    @GetMapping("/topic/{id}")
    @Timed
    public Topic getTopic(@PathVariable Long id)
            throws URISyntaxException {
        return commentService.getTopic(id);
    }

    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.USER + "\")")
    @GetMapping("/topic/{id}/comment")
    @Timed
    public List<Comment> getComments(@PathVariable Long id)
            throws URISyntaxException {
        return commentService.getComments(id);
    }
}
