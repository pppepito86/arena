package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.*;
import com.mycompany.myapp.service.dto.SubmissionDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Submission and its DTO SubmissionDTO.
 */
@Mapper(componentModel = "spring", uses = {UserMapper.class, CompetitionProblemMapper.class})
public interface SubmissionMapper extends EntityMapper<SubmissionDTO, Submission> {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "competitionProblem.id", target = "competitionProblemId")
    SubmissionDTO toDto(Submission submission);

    @Mapping(source = "userId", target = "user")
    @Mapping(source = "competitionProblemId", target = "competitionProblem")
    Submission toEntity(SubmissionDTO submissionDTO);

    default Submission fromId(Long id) {
        if (id == null) {
            return null;
        }
        Submission submission = new Submission();
        submission.setId(id);
        return submission;
    }
}
