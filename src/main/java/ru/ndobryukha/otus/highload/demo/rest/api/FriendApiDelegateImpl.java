package ru.ndobryukha.otus.highload.demo.rest.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.model.UserFriend;
import ru.ndobryukha.otus.highload.demo.domain.service.FriendService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FriendApiDelegateImpl implements FriendApiDelegate {

    private final FriendService friendService;

    @Override
    public Mono<Void> friendDeleteUserIdPut(String friendId, ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .map(UUID::fromString)
                .flatMap(userId -> friendService.delete(new UserFriend(userId, UUID.fromString(friendId))));
    }

    @Override
    public Mono<Void> friendSetUserIdPut(String friendId, ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getName)
                .map(UUID::fromString)
                .flatMap(userId -> friendService.save(new UserFriend(userId, UUID.fromString(friendId))));
    }
}
