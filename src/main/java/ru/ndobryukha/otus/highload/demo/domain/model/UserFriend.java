package ru.ndobryukha.otus.highload.demo.domain.model;

import java.util.UUID;

public record UserFriend(UUID userId, UUID friendId) {}