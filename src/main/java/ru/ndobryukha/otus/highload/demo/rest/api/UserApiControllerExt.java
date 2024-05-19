package ru.ndobryukha.otus.highload.demo.rest.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.service.UserService;

@Controller
@RequiredArgsConstructor
public class UserApiControllerExt {

    private final UserService userService;

    @GetMapping("/registration")
    public Mono<Rendering> login() {
        return Mono.just(
                Rendering.view("registration")
                        .build()
        );
    }

    @GetMapping("/me")
    public Mono<Rendering> me(Authentication authentication) {
        return userService.getById(authentication.getName())
                .map(user -> Rendering.view("profile")
                        .modelAttribute("user", user)
                        .build()
                );
    }

    @GetMapping("/profile/{id}")
    public Mono<Rendering> me(@PathVariable String id) {
        return userService.getById(id)
                .map(user -> Rendering.view("profile")
                        .modelAttribute("user", user)
                        .build()
                );
    }
}
