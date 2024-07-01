package ru.ndobryukha.otus.highload.demo.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.model.UserFriend;
import ru.ndobryukha.otus.highload.demo.domain.repository.UserFriendRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendService {
    private final UserFriendRepository userFriendRepository;
    @Autowired
    @Lazy
    private PostService postService;

    public Mono<Void> save(final UserFriend userFriend) {
        return userFriendRepository.save(userFriend)
                .then(Mono.defer(() -> postService.deleteFeedCached(userFriend.userId())));
    }

    public Mono<Void> delete(final UserFriend userFriend) {
        return userFriendRepository.delete(userFriend)
                .then(Mono.defer(() -> postService.deleteFeedCached(userFriend.userId())));
    }

    public Flux<UserFriend> findAllOf(final UUID friendId) {
        return userFriendRepository.findAllOf(friendId);
    }
}
