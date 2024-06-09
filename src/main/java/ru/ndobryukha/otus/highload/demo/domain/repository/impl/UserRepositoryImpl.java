package ru.ndobryukha.otus.highload.demo.domain.repository.impl;

import io.r2dbc.spi.Parameters;
import io.r2dbc.spi.R2dbcType;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.ndobryukha.otus.highload.demo.domain.model.User;
import ru.ndobryukha.otus.highload.demo.domain.repository.UserRepository;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.BiFunction;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepositoryImpl implements UserRepository {

    private static final BiFunction<Row, RowMetadata, User> MAPPING_FUNCTION = (row, rowMetaData) -> User.builder()
            .id(row.get("id", UUID.class))
            .firstName(row.get("first_name", String.class))
            .secondName(row.get("second_name", String.class))
            .birthdate(row.get("birthdate", LocalDate.class))
            .biography(row.get("biography", String.class))
            .city(row.get("city", String.class))
            .password(row.get("password", String.class))
            .build();

    private final DatabaseClient client;

    @Override
    public Mono<User> findById(UUID id) {
        return client.sql("SELECT * from users WHERE id = :id")
                .bind("id", id)
                .map(MAPPING_FUNCTION)
                .one();
    }

    @Override
    public Mono<UUID> save(User user) {
        return client.sql("""
                            INSERT INTO users (first_name, second_name, birthdate, biography, city, password)
                            VALUES (:firstName, :secondName, :birthdate, :biography, :city, :password)
                        """)
                .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                .bind("firstName", Parameters.in(R2dbcType.VARCHAR, user.firstName()))
                .bind("secondName", Parameters.in(R2dbcType.VARCHAR, user.secondName()))
                .bind("birthdate", Parameters.in(R2dbcType.DATE, user.birthdate()))
                .bind("biography", Parameters.in(R2dbcType.VARCHAR, user.biography()))
                .bind("city", Parameters.in(R2dbcType.VARCHAR, user.city()))
                .bind("password", Parameters.in(R2dbcType.VARCHAR, user.password()))
                .fetch()
                .first()
                .map(r -> (UUID) r.get("id"));
    }

    @Override
    public Flux<User> search(String firstName, String secondName) {
        return client.sql("""
                            SELECT * FROM users
                            WHERE first_name LIKE :firstName and second_name LIKE :secondName
                        """)
                .bind("firstName", firstName + "%")
                .bind("secondName", secondName + "%")
                .map(MAPPING_FUNCTION)
                .all();
    }
}
