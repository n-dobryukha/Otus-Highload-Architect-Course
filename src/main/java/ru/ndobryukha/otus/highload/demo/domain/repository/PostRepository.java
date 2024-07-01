package ru.ndobryukha.otus.highload.demo.domain.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.model.Post;

import java.util.UUID;

public interface PostRepository {
    Mono<UUID> save(UUID userId, String text);
    Mono<Void> update(UUID id, String text);
    Mono<Post> getById(UUID id);
    Flux<Post> getFeed(UUID userId, long offset, long limit);
    Mono<Void> deleteById(UUID id);
}
