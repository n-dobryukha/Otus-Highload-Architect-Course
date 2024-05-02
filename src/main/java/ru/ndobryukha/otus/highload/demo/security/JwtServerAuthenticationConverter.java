package ru.ndobryukha.otus.highload.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private final JwtService jwtService;
    private static final String BEARER = "Bearer ";
    private static final String ACCESS_TOKEN = "ACCESS_TOKEN";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest())
                .mapNotNull(request -> Optional.ofNullable(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                        .filter(header -> header.startsWith(BEARER))
                        .map(header -> header.substring(BEARER.length()))
                        .or(() -> Optional.ofNullable(request.getCookies().getFirst(ACCESS_TOKEN)).map(HttpCookie::getValue))
                        .orElse(null))
                .handle((token, sink) -> {
                    try {
                        sink.next(new JwtToken(token, createUserDetails(token)));
                    } catch (JwtAuthenticationException e) {
                        exchange.getResponse().addCookie(ResponseCookie.from(ACCESS_TOKEN).maxAge(0).build());
                        sink.error(e);
                    }
                });
    }

    private UserDetails createUserDetails(String token) {
        String username = jwtService.extractUsername(token);
        return User.builder()
                .username(username)
                .authorities(createAuthorities(token))
                .password("")
                .build();
    }

    private List<SimpleGrantedAuthority> createAuthorities(String token) {
        return jwtService.extractRoles(token).stream()
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
