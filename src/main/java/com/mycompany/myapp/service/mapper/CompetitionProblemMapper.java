package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.*;
import com.mycompany.myapp.service.dto.CompetitionProblemDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity CompetitionProblem and its DTO CompetitionProblemDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface CompetitionProblemMapper extends EntityMapper<CompetitionProblemDTO, CompetitionProblem> {


    @Mapping(target = "problems", ignore = true)
    @Mapping(target = "competitions", ignore = true)
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
