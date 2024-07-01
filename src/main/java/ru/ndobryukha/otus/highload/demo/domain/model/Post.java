package ru.ndobryukha.otus.highload.demo.domain.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record Post(UUID id, UUID userId, String text) {}
