package co.cobre.cbmm.accounts.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration and functional tests that require containers
 * Provides PostgreSQL, Redis, Kafka and OpenTelemetry Collector test containers
 */
@Slf4j
public abstract class BaseContainerTest {

    private static Network network;
    private static PostgreSQLContainer<?> postgresContainer;
    private static GenericContainer<?> redisContainer;
    private static KafkaContainer kafkaContainer;
    private static GenericContainer<?> otelCollectorContainer;

    // Static block to initialize containers before Spring context loads
    static {
        log.info("Initializing test containers in static block...");
        initContainers();
        log.info("Test containers initialized successfully");
    }

    /**
     * Initialize test containers and configure system properties
     */
    private static void initContainers() {
        if (network == null) {
            log.info("Creating Docker network...");
            network = Network.newNetwork();
            log.info("Docker network created");
        }

        if (postgresContainer == null || !postgresContainer.isRunning()) {
            log.info("Starting PostgreSQL container...");
            postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("cbmm_test")
                .withUsername("test")
                .withPassword("test")
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withReuse(true);
            postgresContainer.start();
            log.info("PostgreSQL container started on port: {}", postgresContainer.getMappedPort(5432));

            // Set PostgreSQL system properties
            System.setProperty("database.url", postgresContainer.getJdbcUrl());
            System.setProperty("database.username", postgresContainer.getUsername());
            System.setProperty("database.password", postgresContainer.getPassword());
            System.setProperty("database.driver-class-name", "org.postgresql.Driver");
            System.setProperty("spring.flyway.url", postgresContainer.getJdbcUrl());
            System.setProperty("spring.flyway.user", postgresContainer.getUsername());
            System.setProperty("spring.flyway.password", postgresContainer.getPassword());
            System.setProperty("spring.flyway.enabled", "true");
            System.setProperty("spring.flyway.baseline-on-migrate", "true");
            log.info("PostgreSQL configuration set: {}", postgresContainer.getJdbcUrl());
        }

        if (redisContainer == null || !redisContainer.isRunning()) {
            log.info("Starting Redis container...");
            redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withNetwork(network)
                .withNetworkAliases("redis")
                .withReuse(true);
            redisContainer.start();
            log.info("Redis container started on port: {}", redisContainer.getMappedPort(6379));

            // Set Redis system properties
            System.setProperty("spring.data.redis.host", redisContainer.getHost());
            System.setProperty("spring.data.redis.port", String.valueOf(redisContainer.getMappedPort(6379)));
            log.info("Redis configuration set: {}:{}", redisContainer.getHost(), redisContainer.getMappedPort(6379));
        }

        if (kafkaContainer == null || !kafkaContainer.isRunning()) {
            log.info("Starting Kafka container...");
            kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
                .asCompatibleSubstituteFor("apache/kafka"))
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withReuse(true);
            kafkaContainer.start();
            log.info("Kafka container started on bootstrap servers: {}", kafkaContainer.getBootstrapServers());

            // Set Kafka system properties
            System.setProperty("spring.kafka.bootstrap-servers", kafkaContainer.getBootstrapServers());
            System.setProperty("spring.kafka.consumer.bootstrap-servers", kafkaContainer.getBootstrapServers());
            System.setProperty("spring.kafka.producer.bootstrap-servers", kafkaContainer.getBootstrapServers());
            log.info("Kafka configuration set: {}", kafkaContainer.getBootstrapServers());
        }

        if (otelCollectorContainer == null || !otelCollectorContainer.isRunning()) {
            log.info("Starting OpenTelemetry Collector container...");
            otelCollectorContainer = new GenericContainer<>(DockerImageName.parse("otel/opentelemetry-collector:0.91.0"))
                .withExposedPorts(4317, 4318, 8888, 8889)
                .withNetwork(network)
                .withNetworkAliases("otel-collector")
                .withCommand("--config=/etc/otel-collector-config.yaml")
                .withReuse(true);
            otelCollectorContainer.start();
            log.info("OpenTelemetry Collector container started on gRPC port: {}, HTTP port: {}",
                otelCollectorContainer.getMappedPort(4317),
                otelCollectorContainer.getMappedPort(4318));

            // Set OpenTelemetry system properties
            String otlpEndpoint = String.format("http://%s:%d",
                otelCollectorContainer.getHost(),
                otelCollectorContainer.getMappedPort(4317));
            System.setProperty("management.otlp.tracing.endpoint", otlpEndpoint);
            System.setProperty("management.tracing.sampling.probability", "1.0");
            log.info("OpenTelemetry configuration set: {}", otlpEndpoint);
        }

        // Set JPA/Hibernate system properties for tests
        System.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        System.setProperty("spring.jpa.show-sql", "false");
        System.setProperty("spring.jpa.properties.hibernate.format_sql", "false");

        // Disable unnecessary features for tests
        System.setProperty("management.endpoints.web.exposure.include", "health,info");
        System.setProperty("management.health.livenessState.enabled", "false");
        System.setProperty("management.health.readinessState.enabled", "false");
    }

    /**
     * Shutdown test containers
     */
    protected static void shutdownContainers() {
        if (otelCollectorContainer != null && otelCollectorContainer.isRunning()) {
            log.info("Stopping OpenTelemetry Collector container...");
            otelCollectorContainer.stop();
        }
        if (kafkaContainer != null && kafkaContainer.isRunning()) {
            log.info("Stopping Kafka container...");
            kafkaContainer.stop();
        }
        if (redisContainer != null && redisContainer.isRunning()) {
            log.info("Stopping Redis container...");
            redisContainer.stop();
        }
        if (postgresContainer != null && postgresContainer.isRunning()) {
            log.info("Stopping PostgreSQL container...");
            postgresContainer.stop();
        }
        if (network != null) {
            log.info("Closing Docker network...");
            network.close();
        }
    }

    /**
     * Configure dynamic properties for Spring Test context
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        if (postgresContainer != null && postgresContainer.isRunning()) {
            registry.add("database.url", postgresContainer::getJdbcUrl);
            registry.add("database.username", postgresContainer::getUsername);
            registry.add("database.password", postgresContainer::getPassword);
            registry.add("database.driver-class-name", () -> "org.postgresql.Driver");

            // Flyway configuration
            registry.add("spring.flyway.url", postgresContainer::getJdbcUrl);
            registry.add("spring.flyway.user", postgresContainer::getUsername);
            registry.add("spring.flyway.password", postgresContainer::getPassword);
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.flyway.baseline-on-migrate", () -> "true");

            log.info("PostgreSQL configuration set: {}", postgresContainer.getJdbcUrl());
        }

        // Redis configuration
        if (redisContainer != null && redisContainer.isRunning()) {
            registry.add("spring.data.redis.host", redisContainer::getHost);
            registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());

            log.info("Redis configuration set: {}:{}",
                redisContainer.getHost(),
                redisContainer.getMappedPort(6379));
        }

        // Kafka configuration
        if (kafkaContainer != null && kafkaContainer.isRunning()) {
            registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
            registry.add("spring.kafka.consumer.bootstrap-servers", kafkaContainer::getBootstrapServers);
            registry.add("spring.kafka.producer.bootstrap-servers", kafkaContainer::getBootstrapServers);

            log.info("Kafka configuration set: {}", kafkaContainer.getBootstrapServers());
        }

        // OpenTelemetry configuration
        if (otelCollectorContainer != null && otelCollectorContainer.isRunning()) {
            String otlpEndpoint = String.format("http://%s:%d",
                otelCollectorContainer.getHost(),
                otelCollectorContainer.getMappedPort(4317));

            registry.add("management.otlp.tracing.endpoint", () -> otlpEndpoint);
            registry.add("management.tracing.sampling.probability", () -> "1.0");

            log.info("OpenTelemetry configuration set: {}", otlpEndpoint);
        }

        // JPA/Hibernate configuration for tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");

        // Disable unnecessary features for tests
        registry.add("management.endpoints.web.exposure.include", () -> "health,info");
        registry.add("management.health.livenessState.enabled", () -> "false");
        registry.add("management.health.readinessState.enabled", () -> "false");
    }
}

