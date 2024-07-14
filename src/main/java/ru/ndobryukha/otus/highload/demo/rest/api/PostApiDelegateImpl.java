package ru.ndobryukha.otus.highload.demo.rest.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.service.PostService;
import ru.ndobryukha.otus.highload.demo.rest.model.Post;
import ru.ndobryukha.otus.highload.demo.rest.model.PostCreatePostRequest;
import ru.ndobryukha.otus.highload.demo.rest.model.PostUpdatePutRequest;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostApiDelegateImpl implements PostApiDelegate {

    private final PostService postService;

    @Override
    public Mono<String> postCreatePost(Mono<PostCreatePostRequest> postCreatePostRequest, ServerWebExchange exchange) {
        return getCurrentUserId()
                .flatMap(userId -> postCreatePostRequest.map(PostCreatePostRequest::getText)
                        .flatMap(text -> postService.save(userId, text))
                )
                .map(UUID::toString);
    }

    @Override
    public Mono<Void> postDeleteIdPut(String id, ServerWebExchange exchange) {
        return getCurrentUserId()
                .flatMap(userId -> postService.deleteById(userId, id));
    }

    @Override
    public Flux<Post> postFeedGet(BigDecimal offset, BigDecimal limit, ServerWebExchange exchange) {
        long off = offset.longValue();
        long lim = limit.longValue();
        return getCurrentUserId()
                .flatMapMany(userId -> off + lim <= 1000
                        ? postService.getFeedCached(userId).skip(off).take(lim)
                        : postService.getFeed(userId, off, lim)
                );
    }

    @Override
    public Mono<Post> postGetIdGet(String id, ServerWebExchange exchange) {
        return postService.getById(id);
    }

    @Override
    public Mono<Void> postUpdatePut(Mono<PostUpdatePutRequest> postUpdatePutRequest, ServerWebExchange exchange) {
        return getCurrentUserId()
                .flatMap(userId -> postUpdatePutRequest.flatMap(req -> postService.update(userId, req.getId(), req.getText())));
    }

    private Mono<UUID> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .map(UUID::fromString);
    }
}
