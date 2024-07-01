package ru.ndobryukha.otus.highload.demo.domain.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.model.UserFriend;

import java.util.UUID;

public interface UserFriendRepository {
    Mono<Void> save(UserFriend userFriend);
    Mono<Void> delete(UserFriend userFriend);
    Flux<UserFriend> findAllOf(final UUID friendId);
}
