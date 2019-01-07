package com.olimpiici.arena.service.mapper;

import com.olimpiici.arena.domain.*;
import com.olimpiici.arena.service.dto.TagCollectionDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity TagCollection and its DTO TagCollectionDTO.
 */
@Mapper(componentModel = "spring", uses = {})
public interface TagCollectionMapper extends EntityMapper<TagCollectionDTO, TagCollection> {



    default TagCollection fromId(Long id) {
        if (id == null) {
            return null;
        }
        TagCollection tagCollection = new TagCollection();
        tagCollection.setId(id);
        return tagCollection;
    }
}
