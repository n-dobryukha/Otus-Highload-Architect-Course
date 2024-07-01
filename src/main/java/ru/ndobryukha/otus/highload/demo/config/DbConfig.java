package ru.ndobryukha.otus.highload.demo.config;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.r2dbc.connection.lookup.AbstractRoutingConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class DbConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.r2dbc")
    R2dbcProperties masterR2dbcProperties() {
        return new R2dbcProperties();
    }

    @Bean
    @ConfigurationProperties("spring.r2dbc.replicas")
    R2dbcProperties replicasR2dbcProperties() {
        return new R2dbcProperties();
    }

    @Bean
    @Primary
    public DatabaseClient masterDatabaseClient(ConnectionFactory connectionFactory) {
        return DatabaseClient.builder()
                .connectionFactory(connectionFactory)
                .namedParameters(true)
                .build();
    }

    @Bean
    public DatabaseClient replicasDatabaseClient(@Qualifier("replicasR2dbcProperties") R2dbcProperties replicasR2dbcProperties,
                                                 ConnectionFactory masterConnectionFactory) {
        ConnectionFactory connectionFactory;
        if (StringUtils.isNoneEmpty(replicasR2dbcProperties.getUrl())) {
            ConnectionFactoryOptions options = ConnectionFactoryOptions.parse(replicasR2dbcProperties.getUrl());
            String[] hosts = ((String) options.getValue(ConnectionFactoryOptions.HOST)).split(",");
            Map<String, ConnectionPool> connectionPoolMap = Stream.of(hosts)
                    .collect(Collectors.toMap(host -> host,
                            host -> {
                                String[] host_port = host.split(":");
                                ConnectionFactoryOptions.Builder builder = options.mutate()
                                        .option(ConnectionFactoryOptions.HOST, host_port[0])
                                        .option(ConnectionFactoryOptions.PORT, Integer.parseInt(host_port[1]))
                                        .option(ConnectionFactoryOptions.USER, replicasR2dbcProperties.getUsername())
                                        .option(ConnectionFactoryOptions.PASSWORD, replicasR2dbcProperties.getPassword());
                                ConnectionFactory connectionFactoryWithOptions = ConnectionFactoryBuilder.withOptions(builder)
                                        .build();
                                R2dbcProperties.Pool pool = replicasR2dbcProperties.getPool();
                                PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
                                ConnectionPoolConfiguration.Builder connectionPoolbuilder = ConnectionPoolConfiguration
                                        .builder(connectionFactoryWithOptions);
                                map.from(pool.getMaxIdleTime()).to(connectionPoolbuilder::maxIdleTime);
                                map.from(pool.getMaxLifeTime()).to(connectionPoolbuilder::maxLifeTime);
                                map.from(pool.getMaxAcquireTime()).to(connectionPoolbuilder::maxAcquireTime);
                                map.from(pool.getMaxCreateConnectionTime()).to(connectionPoolbuilder::maxCreateConnectionTime);
                                map.from(pool.getInitialSize()).to(connectionPoolbuilder::initialSize);
                                map.from(pool.getMaxSize()).to(connectionPoolbuilder::maxSize);
                                map.from(pool.getValidationQuery()).whenHasText().to(connectionPoolbuilder::validationQuery);
                                map.from(pool.getValidationDepth()).to(connectionPoolbuilder::validationDepth);
                                map.from(pool.getMinIdle()).to(connectionPoolbuilder::minIdle);
                                map.from(pool.getMaxValidationTime()).to(connectionPoolbuilder::maxValidationTime);
                                return new ConnectionPool(connectionPoolbuilder.build());
                            }));
            RoundRobinConnectionFactory routingConnectionFactory = new RoundRobinConnectionFactory(connectionPoolMap.keySet());
            routingConnectionFactory.setTargetConnectionFactories(connectionPoolMap);
            routingConnectionFactory.setDefaultTargetConnectionFactory(masterConnectionFactory);
            routingConnectionFactory.initialize();
            connectionFactory = routingConnectionFactory;
        } else {
            connectionFactory = masterConnectionFactory;
        }
        return DatabaseClient.builder()
                .connectionFactory(connectionFactory)
                .namedParameters(true)
                .build();
    }

    static class RoundRobinConnectionFactory extends AbstractRoutingConnectionFactory {

        private final Set<String> hosts;

        public RoundRobinConnectionFactory(Set<String> hosts) {
            this.hosts = hosts;
        }

        private volatile String lastKey = null;


        @Override
        protected Mono<Object> determineCurrentLookupKey() {
            lastKey = hosts.stream().filter(host -> !host.equals(lastKey)).findFirst().orElseGet(() -> hosts.iterator().next());
            return Mono.just(lastKey);
        }
    }
}
