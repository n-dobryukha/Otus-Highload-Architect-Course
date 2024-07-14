package ru.ndobryukha.otus.highload.demo.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface PostMapper {
    @Mapping(target = "authorUserId", source = "userId")
    ru.ndobryukha.otus.highload.demo.rest.model.Post map(ru.ndobryukha.otus.highload.demo.domain.model.Post post);
}
