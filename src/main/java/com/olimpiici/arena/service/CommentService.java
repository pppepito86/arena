package com.olimpiici.arena.service;

import java.time.ZonedDateTime;
import java.util.List;

import com.olimpiici.arena.domain.Comment;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.ProblemTopic;
import com.olimpiici.arena.domain.Topic;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.CommentRepository;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.ProblemTopicRepository;
import com.olimpiici.arena.repository.TopicRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service ementation for managing Tag.
 */
@Service
@Transactional
public class CommentService {

    private final Logger log = LoggerFactory.getLogger(CommentService.class);

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private CompetitionProblemRepository competitionProblemRepository;

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private ProblemTopicRepository problemTopicRepository;

	public Topic getOrCreateTopicForCompProblem(Long compProblemId)  {
		CompetitionProblem cp = competitionProblemRepository.getOne(compProblemId);
		Topic topic = cp.getTopic();
		if (topic == null) {
			topic = new Topic();
			topic.setTitle(cp.getProblem().getTitle());
			topic.setCreatedDate(ZonedDateTime.now());
			topic = topicRepository.save(topic);

			ProblemTopic pt = new ProblemTopic();
			pt.setTopic(topic);
			pt.setCompetitionProblem(cp.getId());
			problemTopicRepository.save(pt);
		}
		return topic;
	}

	public Topic getTopic(Long id)  {
		return topicRepository.getOne(id);
	}

	public Comment postCommentToTopic(Long topicId, User author, String content) {
		Comment comment = new Comment();
		comment.setTopicId(topicId);
		comment.setAuthor(author);
		comment.setContent(content);
		comment.setPostedDate(ZonedDateTime.now());
		comment = commentRepository.save(comment);
		return comment;
	}

	public List<Comment> getComments(Long topicId) {
		return topicRepository.getOne(topicId).getComments();
	}

	public void censureComment(Long commentId) {
		// Comment comment = commentRepository.getOne(commentId);
		// TODO
	}
	
}
