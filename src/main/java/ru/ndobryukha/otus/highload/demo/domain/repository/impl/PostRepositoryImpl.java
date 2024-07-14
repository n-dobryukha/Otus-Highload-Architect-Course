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
import ru.ndobryukha.otus.highload.demo.domain.model.Post;
import ru.ndobryukha.otus.highload.demo.domain.repository.PostRepository;

import java.util.UUID;
import java.util.function.BiFunction;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PostRepositoryImpl implements PostRepository {

    private static final BiFunction<Row, RowMetadata, Post> MAPPING_FUNCTION = (row, rowMetaData) -> Post.builder()
            .id(row.get("id", UUID.class))
            .userId(row.get("user_id", UUID.class))
            .text(row.get("text", String.class))
            .build();

    private final DatabaseClient client;

    @Override
    public Mono<UUID> save(UUID userId, String text) {
        return client.sql("""
                            INSERT INTO posts (user_id, text)
                            VALUES (:userId, :text)
                        """)
                .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
                .bind("userId", userId)
                .bind("text", Parameters.in(R2dbcType.VARCHAR, text))
                .fetch()
                .first()
                .map(r -> (UUID) r.get("id"));
    }

    @Override
    public Mono<Void> update(UUID id, String text) {
        return client.sql("UPDATE posts SET text = :text, modified_at = now() WHERE id = :id")
                .bind("id", id)
                .bind("text", Parameters.in(R2dbcType.VARCHAR, text))
                .then();
    }

    @Override
    public Mono<Post> getById(UUID id) {
        return client.sql("SELECT * from posts WHERE id = :id")
                .bind("id", id)
                .map(MAPPING_FUNCTION)
                .one();
    }

    @Override
    public Flux<Post> getFeed(UUID userId, long offset, long limit) {
        return client.sql("""
                SELECT p.id, p.user_id, p.text FROM posts p
                LEFT JOIN user_friends f ON p.user_id = f.friend_id
                LEFT JOIN users u ON u.id = f.user_id
                WHERE u.id = :userId
                ORDER BY modified_at DESC
                LIMIT :limit OFFSET :offset 
        """)
                .bind("userId", userId)
                .bind("offset", offset)
                .bind("limit", limit)
                .map(MAPPING_FUNCTION)
                .all();
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return client.sql("DELETE FROM posts WHERE id = :id")
                .bind("id", id)
                .then();
    }
}
