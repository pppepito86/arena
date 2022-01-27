package com.olimpiici.arena.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.olimpiici.arena.domain.Problem;
import com.olimpiici.arena.service.dto.ProblemDTO;

/**
 * Mapper for the entity Problem and its DTO ProblemDTO.
 */
@Mapper(componentModel = "spring", uses = {TagCollectionMapper.class, CompetitionMapper.class})
public interface ProblemMapper extends EntityMapper<ProblemDTO, Problem> {

    @Override
	@Mapping(source = "tags.id", target = "tagsId")
    @Mapping(source = "canonicalCompetitionProblem.competition.id", target = "competitionId")
    @Mapping(source = "competition.label", target = "competitionLabel")
    @Mapping(source = "canonicalCompetitionProblem.id", target = "canonicalCompetitionProblemId")
    ProblemDTO toDto(Problem problem);

    @Override
	@Mapping(source = "tagsId", target = "tags")
    @Mapping(source = "competitionId", target = "competition")
    Problem toEntity(ProblemDTO problemDTO);

    default Problem fromId(Long id) {
        if (id == null) {
            return null;
        }
        Problem problem = new Problem();
        problem.setId(id);
        return problem;
    }
}
