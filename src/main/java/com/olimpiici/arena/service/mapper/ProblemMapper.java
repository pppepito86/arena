package com.olimpiici.arena.service.mapper;

import com.olimpiici.arena.domain.*;
import com.olimpiici.arena.service.dto.ProblemDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity Problem and its DTO ProblemDTO.
 */
@Mapper(componentModel = "spring", uses = {TagCollectionMapper.class})
public interface ProblemMapper extends EntityMapper<ProblemDTO, Problem> {

    @Mapping(source = "tags.id", target = "tagsId")
    ProblemDTO toDto(Problem problem);

    @Mapping(source = "tagsId", target = "tags")
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
