package ru.ndobryukha.otus.highload.demo.domain.model;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record User (UUID id, String firstName, String secondName, LocalDate birthdate, String biography, String city, String password) {}