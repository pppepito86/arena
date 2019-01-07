package com.olimpiici.arena.service.mapper;

import com.olimpiici.arena.domain.*;
import com.olimpiici.arena.service.dto.TagCollectionTagDTO;

import org.mapstruct.*;

/**
 * Mapper for the entity TagCollectionTag and its DTO TagCollectionTagDTO.
 */
@Mapper(componentModel = "spring", uses = {TagCollectionMapper.class, TagMapper.class})
public interface TagCollectionTagMapper extends EntityMapper<TagCollectionTagDTO, TagCollectionTag> {

    @Mapping(source = "collection.id", target = "collectionId")
    @Mapping(source = "tag.id", target = "tagId")
    TagCollectionTagDTO toDto(TagCollectionTag tagCollectionTag);

    @Mapping(source = "collectionId", target = "collection")
    @Mapping(source = "tagId", target = "tag")
    TagCollectionTag toEntity(TagCollectionTagDTO tagCollectionTagDTO);

    default TagCollectionTag fromId(Long id) {
        if (id == null) {
            return null;
        }
        TagCollectionTag tagCollectionTag = new TagCollectionTag();
        tagCollectionTag.setId(id);
        return tagCollectionTag;
    }
}
