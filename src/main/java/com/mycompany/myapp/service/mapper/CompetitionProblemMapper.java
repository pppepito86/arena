package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.*;
import com.mycompany.myapp.service.dto.CompetitionProblemDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity CompetitionProblem and its DTO CompetitionProblemDTO.
 */
@Mapper(componentModel = "spring", uses = {ProblemMapper.class, CompetitionMapper.class})
public interface CompetitionProblemMapper extends EntityMapper<CompetitionProblemDTO, CompetitionProblem> {

    @Mapping(source = "problem.id", target = "problemId")
    @Mapping(source = "competition.id", target = "competitionId")
    CompetitionProblemDTO toDto(CompetitionProblem competitionProblem);

    @Mapping(source = "problemId", target = "problem")
    @Mapping(source = "competitionId", target = "competition")
    CompetitionProblem toEntity(CompetitionProblemDTO competitionProblemDTO);

    default CompetitionProblem fromId(Long id) {
        if (id == null) {
            return null;
        }
        CompetitionProblem competitionProblem = new CompetitionProblem();
        competitionProblem.setId(id);
        return competitionProblem;
    }
}
