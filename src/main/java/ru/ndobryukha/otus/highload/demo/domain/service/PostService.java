package ru.ndobryukha.otus.highload.demo.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.mapper.PostMapper;
import ru.ndobryukha.otus.highload.demo.domain.model.UserFriend;
import ru.ndobryukha.otus.highload.demo.domain.repository.PostRepository;
import ru.ndobryukha.otus.highload.demo.rest.model.Post;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository repository;
    private final PostMapper mapper;
    private final FriendService friendService;
    @Autowired
    @Lazy
    private PostService self;

    public Mono<UUID> save(UUID userId, String text) {
        Mono<UUID> result = repository.save(userId, text);
        friendService.findAllOf(userId).map(UserFriend::userId).subscribe(self::deleteFeedCached);
        return result;
    }

    public Mono<Void> update(UUID userId, String uuid, String text) {
        return getModel(uuid)
                .filter(post -> userId.equals(post.userId()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, uuid)))
                .flatMap(post -> {
                    Mono<Void> result = repository.update(post.id(), text);
                    friendService.findAllOf(userId).map(UserFriend::userId).subscribe(self::deleteFeedCached);
                    return result;
                });
    }

    public Mono<Post> getById(String uuid) {
        return getModel(uuid).map(mapper::map);
    }

    private Mono<ru.ndobryukha.otus.highload.demo.domain.model.Post > getModel(String uuid) {
        return Mono.just(uuid)
                .map(UUID::fromString)
                .onErrorMap(e -> new ResponseStatusException(HttpStatus.NOT_FOUND, uuid))
                .flatMap(repository::getById)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, uuid)))
                .doOnError(ResponseStatusException.class, e -> log.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage()));
    }

    @Cacheable(cacheNames = "friends-post-feed", key = "#userId")
    public Flux<Post> getFeedCached(UUID userId) {
        return getFeed(userId, 0, 1000);
    }

    public Flux<Post> getFeed(UUID userId, long offset, long limit) {
        return repository.getFeed(userId, offset, limit)
                .map(mapper::map);
    }

    public Mono<Void> deleteById(UUID userId, String uuid) {
        return getModel(uuid)
                .filter(post -> userId.equals(post.userId()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, uuid)))
                .flatMap(post -> {
                    Mono<Void> result = repository.deleteById(post.id());
                    friendService.findAllOf(userId).map(UserFriend::userId).subscribe(self::deleteFeedCached);
                    return result;
                });
    }

    @CacheEvict(cacheNames = "friends-post-feed", key = "#userId", beforeInvocation = true)
    public Mono<Void> deleteFeedCached(UUID userId) {
        log.info("{}: delete cache for the user", userId);
        return Mono.empty();
    }
}
