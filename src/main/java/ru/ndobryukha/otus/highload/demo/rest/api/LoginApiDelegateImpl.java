package ru.ndobryukha.otus.highload.demo.rest.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.rest.model.LoginPost200Response;
import ru.ndobryukha.otus.highload.demo.rest.model.LoginPostRequest;
import ru.ndobryukha.otus.highload.demo.security.JwtService;

import java.net.URI;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoginApiDelegateImpl implements LoginApiDelegate {

    private final PasswordEncoder passwordEncoder;
    private final ReactiveUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    public Mono<ResponseEntity<LoginPost200Response>> loginPost(Mono<LoginPostRequest> request, ServerWebExchange exchange) {
        return request.flatMap(it -> userDetailsService.findByUsername(it.getId())
                    .filter(u -> passwordEncoder.matches(it.getPassword(), u.getPassword()))
                )
                .map(jwtService::generateToken)
                .flatMap(token -> exchange.getSession()
                        .map(webSession -> Optional.ofNullable(webSession.getAttribute("SPRING_SECURITY_SAVED_REQUEST"))                                     .map(String::valueOf)
                                .map(location -> ResponseEntity.ok().location(URI.create(location)))
                                .orElseGet(ResponseEntity::ok)
                                .header(HttpHeaders.SET_COOKIE, "ACCESS_TOKEN=" + token)
                                .body(new LoginPost200Response().token(token))
                        )
                )
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
    }

}
