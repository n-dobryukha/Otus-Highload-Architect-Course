package ru.ndobryukha.otus.highload.demo.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.mapper.UserMapper;
import ru.ndobryukha.otus.highload.demo.domain.repository.UserRepository;
import ru.ndobryukha.otus.highload.demo.rest.model.User;
import ru.ndobryukha.otus.highload.demo.rest.model.UserRegisterPost200Response;
import ru.ndobryukha.otus.highload.demo.rest.model.UserRegisterPostRequest;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements ReactiveUserDetailsService {

    private final UserMapper mapper;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public Mono<UserDetails> findByUsername(String uuid) {
        return Mono.just(uuid)
                .map(UUID::fromString)
                .onErrorMap(e -> new UsernameNotFoundException(uuid))
                .flatMap(repository::findById)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(uuid)))
                .doOnError(UsernameNotFoundException.class, e -> log.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage()))
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.id().toString(), user.password(), Collections.emptySet())
                );
    }

    public Mono<User> getById(String uuid) {
        return Mono.just(uuid)
                .map(UUID::fromString)
                .onErrorMap(e -> new ResponseStatusException(HttpStatus.NOT_FOUND, uuid))
                .flatMap(repository::findById)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, uuid)))
                .doOnError(ResponseStatusException.class, e -> log.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage()))
                .map(mapper::map);
    }

    public Mono<UserRegisterPost200Response> register(Mono<UserRegisterPostRequest> request) {
        return request
                .map(r -> mapper.map(r, passwordEncoder))
                .flatMap(repository::save)
                .map(id -> UserRegisterPost200Response.builder().userId(id.toString()).build());
    }

    public Flux<User> search(String firstName, String lastName) {
        return repository.search(firstName, lastName)
                .map(mapper::map);
    }

}
