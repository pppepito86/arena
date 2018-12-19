package com.mycompany.myapp.service.mapper;

import com.mycompany.myapp.domain.*;
import com.mycompany.myapp.service.dto.CompetitionDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Competition and its DTO CompetitionDTO.
 */
@Mapper(componentModel = "spring", uses = {CompetitionProblemMapper.class})
public interface CompetitionMapper extends EntityMapper<CompetitionDTO, Competition> {

    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "competitionProblem.id", target = "competitionProblemId")
    CompetitionDTO toDto(Competition competition);

    @Mapping(source = "parentId", target = "parent")
    @Mapping(source = "competitionProblemId", target = "competitionProblem")
    Competition toEntity(CompetitionDTO competitionDTO);

    default Competition fromId(Long id) {
        if (id == null) {
            return null;
        }
        Competition competition = new Competition();
        competition.setId(id);
        return competition;
    }
}
