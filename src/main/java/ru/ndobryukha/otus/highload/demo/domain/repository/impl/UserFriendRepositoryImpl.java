package ru.ndobryukha.otus.highload.demo.domain.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.model.UserFriend;
import ru.ndobryukha.otus.highload.demo.domain.repository.UserFriendRepository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserFriendRepositoryImpl implements UserFriendRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Void> save(UserFriend userFriend) {
        return databaseClient.sql("INSERT INTO user_friends (user_id, friend_id) values (:userId, :friendId)")
                .bind("userId", userFriend.userId())
                .bind("friendId", userFriend.friendId())
                .then();
    }

    @Override
    public Mono<Void> delete(UserFriend userFriend) {
        return databaseClient.sql("DELETE FROM user_friends WHERE user_id = :userId AND friend_id = :friendId")
                .bind("userId", userFriend.userId())
                .bind("friendId", userFriend.friendId())
                .then();
    }

    @Override
    public Flux<UserFriend> findAllOf(UUID friendId) {
        return databaseClient.sql("SELECT * FROM user_friends where friend_id = :friendId")
                .bind("friendId", friendId)
                .map((row, rowMetaData) -> new UserFriend(
                        row.get("user_id", UUID.class),
                        row.get("friend_id", UUID.class))
                )
                .all();
    }
}
