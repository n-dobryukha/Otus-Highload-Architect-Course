package ru.ndobryukha.otus.highload.demo.domain.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.model.User;

import java.util.UUID;

public interface UserRepository {

    Mono<User> findById(UUID id);
    Mono<UUID> save(User user);
    Flux<User> search(String firstName, String lastName);
}
