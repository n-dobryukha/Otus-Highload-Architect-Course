package ru.ndobryukha.otus.highload.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.security.JwtService;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    public SecurityWebFilterChain apiSecurityWebFilterChain(ServerHttpSecurity http,
                                                            AuthenticationWebFilter jwtAuthenticationWebFilter) {
        return http
                .securityMatcher(new OrServerWebExchangeMatcher(
                        new PathPatternParserServerWebExchangeMatcher("/user/**"),
                        new PathPatternParserServerWebExchangeMatcher("/login", HttpMethod.POST)
                ))
                .authorizeExchange(exchangeSpec -> exchangeSpec
                        .pathMatchers(HttpMethod.POST, "/login", "/user/register").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         AuthenticationWebFilter jwtAuthenticationWebFilter,
                                                         ReactiveUserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder,
                                                         JwtService jwtService) {
        UserDetailsRepositoryReactiveAuthenticationManager formLoginAuthenticationManager = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        formLoginAuthenticationManager.setPasswordEncoder(passwordEncoder);

        return http
                .authorizeExchange(exchangeSpec -> exchangeSpec
                        .pathMatchers(HttpMethod.GET, "/", "/favicon.ico", "/login", "/registration", "/webjars/**",
                                "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(formLoginSpec -> formLoginSpec
                        .loginPage("/login")
                        .requiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/login-form"))
                        .authenticationManager(formLoginAuthenticationManager)
                        .authenticationSuccessHandler(
                                new ServerAuthenticationSuccessHandler() {
                                    private final ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

                                    @Override
                                    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
                                        String token = jwtService.generateToken((UserDetails) authentication.getPrincipal());
                                        ServerWebExchange exchange = webFilterExchange.getExchange();
                                        exchange.getResponse().addCookie(ResponseCookie.from("ACCESS_TOKEN", token).build());
                                        return redirectStrategy.sendRedirect(exchange, URI.create("/me"));
                                    }
                                }

                        )
                )
                .logout(logoutSpec -> logoutSpec.logoutUrl("/logout")
                        .logoutHandler(
                                new DelegatingServerLogoutHandler(
                                        new SecurityContextServerLogoutHandler(),
                                        (exchange, authentication) -> {
                                            ServerHttpResponse response = exchange.getExchange().getResponse();
                                            response.setStatusCode(HttpStatus.FOUND);
                                            response.getHeaders().setLocation(URI.create("/login.html?logout"));
                                            response.addCookie(ResponseCookie.from("ACCESS_TOKEN").maxAge(0).build());
                                            return exchange.getExchange().getSession()
                                                    .flatMap(WebSession::invalidate);
                                        }
                                )
                        )
                )
                .build();
    }

    @Bean
    public AuthenticationWebFilter jwtAuthenticationWebFilter(ReactiveAuthenticationManager authenticationManager,
                                                              ServerAuthenticationConverter authenticationConverter) {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(authenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(authenticationConverter);
        return authenticationWebFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
