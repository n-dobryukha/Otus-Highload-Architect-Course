package ru.ndobryukha.otus.highload.demo.rest.api;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class LoginApiControllerExt {

    @GetMapping("/login")
    public Mono<Rendering> login() {
        return Mono.just(Rendering.view("login").build());
    }

}
