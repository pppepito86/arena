package com.olimpiici.arena.service.impl;

import com.olimpiici.arena.service.CompetitionService;
import com.olimpiici.arena.service.SubmissionService;
import com.olimpiici.arena.domain.Competition;
import com.olimpiici.arena.domain.CompetitionProblem;
import com.olimpiici.arena.domain.Submission;
import com.olimpiici.arena.domain.User;
import com.olimpiici.arena.repository.CompetitionProblemRepository;
import com.olimpiici.arena.repository.CompetitionRepository;
import com.olimpiici.arena.repository.SubmissionRepository;
import com.olimpiici.arena.repository.UserRepository;
import com.olimpiici.arena.service.dto.SubmissionDTO;
import com.olimpiici.arena.service.mapper.SubmissionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for managing Submission.
 */
@Service
@Transactional
public class SubmissionServiceImpl implements SubmissionService {

    private final Logger log = LoggerFactory.getLogger(SubmissionServiceImpl.class);

    private final SubmissionRepository submissionRepository;
    
    private final UserRepository userRepository;
    
    private final CompetitionRepository competitionRepository;
    
    private final CompetitionProblemRepository competitionProblemRepository;

    private final SubmissionMapper submissionMapper;

    public SubmissionServiceImpl(SubmissionRepository submissionRepository, 
    		SubmissionMapper submissionMapper,
    		CompetitionProblemRepository competitionProblemRepository,
    		UserRepository userRepository,
    		CompetitionRepository competitionRepository) {
        this.submissionRepository = submissionRepository;
        this.submissionMapper = submissionMapper;
        this.competitionProblemRepository = competitionProblemRepository;
        this.userRepository = userRepository;
        this.competitionRepository = competitionRepository;
    }

    /**
     * Save a submission.
     *
     * @param submissionDTO the entity to save
     * @return the persisted entity
     */
    @Override
    public SubmissionDTO save(SubmissionDTO submissionDTO) {
        log.debug("Request to save Submission : {}", submissionDTO);

        Submission submission = submissionMapper.toEntity(submissionDTO);
        submission = submissionRepository.save(submission);
        return submissionMapper.toDto(submission);
    }

    /**
     * Get all the submissions.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SubmissionDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Submissions");
        return submissionRepository.findAll(pageable)
            .map(submissionMapper::toDto);
    }


    /**
     * Get one submission by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<SubmissionDTO> findOne(Long id) {
        log.debug("Request to get Submission : {}", id);
        return submissionRepository.findById(id)
            .map(submissionMapper::toDto)
            .map(dto -> {
            	dto.setCode(findSubmissionCode(id));
            	return dto;
            });
    }

    /**
     * Delete the submission by id.
     *
     * @param id the id of the entity
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete Submission : {}", id);
        submissionRepository.deleteById(id);
    }

	@Override
	public Page<SubmissionDTO> findSubmissionsByCompetitionProblemAndUser(Long userId, 
			Long competitionProblemId, Pageable pageable) {
		log.debug("Request to get all submissions for CompetitionProblem {}", competitionProblemId);
		User user = userRepository.findById(userId).get();
		return findSubmissionsByCompetitionProblemAndUser(user, competitionProblemId, pageable);
	}
	
	@Override
	public Page<SubmissionDTO> findSubmissionsByCompetitionProblemAndUser(User user, 
			Long competitionProblemId,
			Pageable pageable) {
		CompetitionProblem competitionProblem = competitionProblemRepository
				.findById(competitionProblemId).get();
		
		Page<SubmissionDTO> submissions = submissionRepository
				.findByCompetitionProblemAndUser(competitionProblem, user, pageable)
				.map(submissionMapper::toDto);
		return submissions;
	}
	
	@Override
	public Page<SubmissionDTO> findSubmissionsByUserAndCompetitionProblemIn(User user,
			List<CompetitionProblem> competitionProblems, Pageable pageable) {
		return submissionRepository
				.findByUserAndCompetitionProblemIn(user, competitionProblems, pageable)
				.map(submissionMapper::toDto);
	}

	@Override
	public Page<SubmissionDTO> findSubmissionsByCompetitionProblem(
			Long competitionProblemId, Pageable pageable) {
		CompetitionProblem competitionProblem = 
				competitionProblemRepository.getOne(competitionProblemId);
		Page<SubmissionDTO> submissions = submissionRepository
				.findByCompetitionProblem(competitionProblem, pageable)
				.map(submissionMapper::toDto);
		return submissions;
	}
	
	@Override
	public Page<SubmissionDTO> findSubmissionsByCompetitionProblemIn(
			List<CompetitionProblem> competitionProblems, Pageable pageable) {
		return submissionRepository
				.findByCompetitionProblemIn(competitionProblems, pageable)
				.map(submissionMapper::toDto);
	}
	
	@Override
	public Optional<SubmissionDTO> findSubmissionByVerdict(String verdict) {
		return submissionRepository
				.findByVerdict(verdict)
				.stream()
				.map(submissionMapper::toDto)
				.findFirst();
	}
	
	@Override
	public String findSubmissionCode(Long id) {
		// TODO 
		
		return "#include <iostream>\n" + 
				"using namespace std;\n" + 
				"\n" + 
				"int main() \n" + 
				"{\n" + 
				"    cout << \"Hello, World!\";\n" + 
				"    return 0;\n" + 
				"}";
	}

}
