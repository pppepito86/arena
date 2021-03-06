package com.olimpiici.arena.service.mapper;

import com.olimpiici.arena.domain.*;
import com.olimpiici.arena.service.dto.SubmissionDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Submission and its DTO SubmissionDTO.
 */
@Mapper(componentModel = "spring", uses = {TagCollectionMapper.class, UserMapper.class, CompetitionProblemMapper.class})
public interface SubmissionMapper extends EntityMapper<SubmissionDTO, Submission> {

    @Mapping(source = "tags.id", target = "tagsId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "competitionProblem.id", target = "competitionProblemId")
    @Mapping(source = "competitionProblem.competition.id", target = "competitionId")
    @Mapping(source = "user.firstName", target = "userFirstName")
    @Mapping(source = "user.lastName", target = "userLastName")
    @Mapping(source = "competitionProblem.problem.title", target = "problemName")
    SubmissionDTO toDto(Submission submission);

    @Mapping(source = "tagsId", target = "tags")
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
