package ru.ndobryukha.otus.highload.demo.rest.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.service.UserService;
import ru.ndobryukha.otus.highload.demo.rest.model.User;
import ru.ndobryukha.otus.highload.demo.rest.model.UserRegisterPost200Response;
import ru.ndobryukha.otus.highload.demo.rest.model.UserRegisterPostRequest;

@Component
@RequiredArgsConstructor
public class UserApiDelegateImpl implements UserApiDelegate {

    private final UserService userService;

    @Override
    public Mono<User> userGetIdGet(String id, ServerWebExchange exchange) {
        return userService.getById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @Override
    public Mono<UserRegisterPost200Response> userRegisterPost(Mono<UserRegisterPostRequest> request, ServerWebExchange exchange) {
        return userService.register(request);
    }

    @Override
    public Flux<User> userSearchGet(String firstName, String lastName, ServerWebExchange exchange) {
        return userService.search(firstName, lastName);
    }
}
