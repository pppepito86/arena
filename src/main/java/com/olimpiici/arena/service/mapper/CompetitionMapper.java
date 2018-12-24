package com.olimpiici.arena.service.mapper;

import com.olimpiici.arena.domain.*;
import com.olimpiici.arena.service.dto.CompetitionDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Competition and its DTO CompetitionDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface CompetitionMapper extends EntityMapper<CompetitionDTO, Competition> {

    @Mapping(source = "parent.id", target = "parentId")
    CompetitionDTO toDto(Competition competition);

    @Mapping(source = "parentId", target = "parent")
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
