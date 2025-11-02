package co.cobre.cbmm.accounts.integration.adapters.in.messaging;

import co.cobre.cbmm.accounts.MsAccountsApplication;
import co.cobre.cbmm.accounts.adapters.out.persistence.entity.AccountEntity;
import co.cobre.cbmm.accounts.adapters.out.persistence.entity.CBMMEventEntity;
import co.cobre.cbmm.accounts.adapters.out.persistence.entity.TransactionEntity;
import co.cobre.cbmm.accounts.adapters.out.persistence.repository.AccountJpaRepository;
import co.cobre.cbmm.accounts.adapters.out.persistence.repository.CBMMEventJpaRepository;
import co.cobre.cbmm.accounts.adapters.out.persistence.repository.TransactionJpaRepository;
import co.cobre.cbmm.accounts.base.BaseContainerTest;
import co.cobre.cbmm.accounts.domain.model.AccountStatus;
import co.cobre.cbmm.accounts.domain.model.Currency;
import co.cobre.cbmm.accounts.domain.model.TransactionStatus;
import co.cobre.cbmm.accounts.domain.model.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(
    classes = MsAccountsApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("KafkaEventConsumer Integration Tests")
class KafkaEventConsumerIntegrationTest extends BaseContainerTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

    @Autowired
    private CBMMEventJpaRepository cbmmEventJpaRepository;

    private static final String TOPIC = "cbmm-events-test";
    private static final String ORIGIN_ACCOUNT = "ACC123456789";
    private static final String DESTINATION_ACCOUNT = "ACC987654321";

    @BeforeEach
    void setUp() {
        log.info("Cleaning up database before test...");
        // Clean up database before each test
        cbmmEventJpaRepository.deleteAll();
        transactionJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();

        // Create origin account with MXN currency and balance
        AccountEntity originAccount = new AccountEntity();
        originAccount.setAccountNumber(ORIGIN_ACCOUNT);
        originAccount.setCurrency(Currency.MXN.getCode());
        originAccount.setBalance(new BigDecimal("200000.00"));
        originAccount.setStatus(AccountStatus.ACTIVE.name());
        accountJpaRepository.save(originAccount);
        log.info("Created origin account: {} with balance: {}", ORIGIN_ACCOUNT, originAccount.getBalance());

        // Create destination account with USD currency and zero balance
        AccountEntity destinationAccount = new AccountEntity();
        destinationAccount.setAccountNumber(DESTINATION_ACCOUNT);
        destinationAccount.setCurrency(Currency.USD.getCode());
        destinationAccount.setBalance(BigDecimal.ZERO);
        destinationAccount.setStatus(AccountStatus.ACTIVE.name());
        accountJpaRepository.save(destinationAccount);
        log.info("Created destination account: {} with balance: {}", DESTINATION_ACCOUNT, destinationAccount.getBalance());
    }

    @Test
    @Order(1)
    @DisplayName("Given valid CBMM event, when sent to Kafka, then process successfully and update accounts")
    void givenValidCBMMEvent_whenSentToKafka_thenProcessSuccessfullyAndUpdateAccounts() {
        // Arrange
        String eventJson = """
            {
                "event_id": "cbmm_20250909_000123",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T15:32:10Z",
                "origin": {
                    "account_id": "ACC123456789",
                    "currency": "MXN",
                    "amount": 15000.50
                },
                "destination": {
                    "account_id": "ACC987654321",
                    "currency": "USD",
                    "amount": 880.25
                }
            }
            """;

        log.info("Sending CBMM event to Kafka topic: {}", TOPIC);

        // Act
        kafkaTemplate.send(TOPIC, eventJson);

        // Assert - Wait for async processing
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                // Verify event was persisted
                List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
                assertFalse(events.isEmpty(), "Event should be persisted");
                log.info("Found {} events in database", events.size());

                // Verify origin account balance decreased
                AccountEntity originAccount = accountJpaRepository.findByAccountNumber(ORIGIN_ACCOUNT).orElseThrow();
                assertEquals(new BigDecimal("184999.50"), originAccount.getBalance());
                log.info("Origin account balance updated to: {}", originAccount.getBalance());

                // Verify destination account balance increased
                AccountEntity destinationAccount = accountJpaRepository.findByAccountNumber(DESTINATION_ACCOUNT).orElseThrow();
                assertEquals(new BigDecimal("880.25"), destinationAccount.getBalance());
                log.info("Destination account balance updated to: {}", destinationAccount.getBalance());

                // Verify transactions were created
                List<TransactionEntity> transactions = transactionJpaRepository.findAll();
                assertEquals(2, transactions.size());
                log.info("Found {} transactions in database", transactions.size());

                // Verify debit transaction
                TransactionEntity debitTx = transactions.stream()
                    .filter(tx -> tx.getType().equals(TransactionType.DEBIT.name()))
                    .findFirst()
                    .orElseThrow();
                assertEquals(new BigDecimal("15000.50"), debitTx.getAmount());
                assertEquals(TransactionStatus.COMPLETED.name(), debitTx.getStatus());
                log.info("Debit transaction verified: {} {}", debitTx.getAmount(), debitTx.getCurrency());

                // Verify credit transaction
                TransactionEntity creditTx = transactions.stream()
                    .filter(tx -> tx.getType().equals(TransactionType.CREDIT.name()))
                    .findFirst()
                    .orElseThrow();
                assertEquals(new BigDecimal("880.25"), creditTx.getAmount());
                assertEquals(TransactionStatus.COMPLETED.name(), creditTx.getStatus());
                log.info("Credit transaction verified: {} {}", creditTx.getAmount(), creditTx.getCurrency());
            });

        log.info("Test completed successfully!");
    }

    @Test
    @Order(2)
    @DisplayName("Given duplicate event, when sent to Kafka, then process only once")
    void givenDuplicateEvent_whenSentToKafka_thenProcessOnlyOnce() throws InterruptedException {
        // Arrange
        String eventJson = """
            {
                "event_id": "cbmm_20250909_000456",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T16:00:00Z",
                "origin": {
                    "account_id": "ACC123456789",
                    "currency": "MXN",
                    "amount": 1000.00
                },
                "destination": {
                    "account_id": "ACC987654321",
                    "currency": "USD",
                    "amount": 50.00
                }
            }
            """;

        log.info("Sending duplicate event to Kafka...");

        // Act - Send same event twice
        kafkaTemplate.send(TOPIC, eventJson);
        Thread.sleep(3000); // Wait for first processing
        kafkaTemplate.send(TOPIC, eventJson);

        // Assert
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                // Verify only one event record
                List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
                long eventCount = events.stream()
                    .filter(e -> e.getEventId().equals("cbmm_20250909_000456"))
                    .count();
                assertEquals(1, eventCount, "Should have only one event record");
                log.info("Verified only one event was persisted");

                // Verify origin account balance decreased only once
                AccountEntity originAccount = accountJpaRepository.findByAccountNumber(ORIGIN_ACCOUNT).orElseThrow();
                assertEquals(new BigDecimal("199000.00"), originAccount.getBalance());
                log.info("Origin account balance: {}", originAccount.getBalance());

                // Verify destination account balance increased only once
                AccountEntity destinationAccount = accountJpaRepository.findByAccountNumber(DESTINATION_ACCOUNT).orElseThrow();
                assertEquals(new BigDecimal("50.00"), destinationAccount.getBalance());
                log.info("Destination account balance: {}", destinationAccount.getBalance());

                // Verify only 2 transactions were created (not 4)
                List<TransactionEntity> transactions = transactionJpaRepository.findAll();
                assertEquals(2, transactions.size());
                log.info("Verified only 2 transactions were created");
            });

        log.info("Idempotency test completed successfully!");
    }

    @Test
    @Order(3)
    @DisplayName("Given invalid JSON, when sent to Kafka, then don't process and don't update accounts")
    void givenInvalidJson_whenSentToKafka_thenDontProcessAndDontUpdateAccounts() throws InterruptedException {
        // Arrange
        BigDecimal originBalanceBefore = accountJpaRepository.findByAccountNumber(ORIGIN_ACCOUNT)
            .orElseThrow()
            .getBalance();
        BigDecimal destBalanceBefore = accountJpaRepository.findByAccountNumber(DESTINATION_ACCOUNT)
            .orElseThrow()
            .getBalance();

        String invalidJson = "{ invalid json format }";

        log.info("Sending invalid JSON to Kafka...");

        // Act
        kafkaTemplate.send(TOPIC, invalidJson);

        // Assert - Wait a bit and verify nothing changed
        Thread.sleep(5000);

        AccountEntity originAccount = accountJpaRepository.findByAccountNumber(ORIGIN_ACCOUNT).orElseThrow();
        assertEquals(originBalanceBefore, originAccount.getBalance());
        log.info("Origin account balance unchanged: {}", originAccount.getBalance());

        AccountEntity destinationAccount = accountJpaRepository.findByAccountNumber(DESTINATION_ACCOUNT).orElseThrow();
        assertEquals(destBalanceBefore, destinationAccount.getBalance());
        log.info("Destination account balance unchanged: {}", destinationAccount.getBalance());

        log.info("Invalid JSON test completed successfully!");
    }

    @Test
    @Order(4)
    @DisplayName("Given insufficient balance, when sent to Kafka, then reject transaction")
    void givenInsufficientBalance_whenSentToKafka_thenRejectTransaction() throws InterruptedException {
        // Arrange
        String eventJson = """
            {
                "event_id": "cbmm_20250909_000789",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T17:00:00Z",
                "origin": {
                    "account_id": "ACC123456789",
                    "currency": "MXN",
                    "amount": 999999.00
                },
                "destination": {
                    "account_id": "ACC987654321",
                    "currency": "USD",
                    "amount": 50000.00
                }
            }
            """;

        BigDecimal originBalanceBefore = accountJpaRepository.findByAccountNumber(ORIGIN_ACCOUNT)
            .orElseThrow()
            .getBalance();

        log.info("Sending event with insufficient balance to Kafka...");

        // Act
        kafkaTemplate.send(TOPIC, eventJson);

        // Assert - Wait and verify balance didn't change
        Thread.sleep(5000);

        AccountEntity originAccount = accountJpaRepository.findByAccountNumber(ORIGIN_ACCOUNT).orElseThrow();
        assertEquals(originBalanceBefore, originAccount.getBalance());
        log.info("Origin account balance unchanged: {}", originAccount.getBalance());

        log.info("Insufficient balance test completed successfully!");
    }

    @Test
    @Order(5)
    @DisplayName("Given multiple events, when sent to Kafka, then process all successfully")
    void givenMultipleEvents_whenSentToKafka_thenProcessAllSuccessfully() {
        // Arrange
        String event1 = """
            {
                "event_id": "cbmm_20250909_002001",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T19:00:00Z",
                "origin": {
                    "account_id": "ACC123456789",
                    "currency": "MXN",
                    "amount": 500.00
                },
                "destination": {
                    "account_id": "ACC987654321",
                    "currency": "USD",
                    "amount": 25.00
                }
            }
            """;

        String event2 = """
            {
                "event_id": "cbmm_20250909_002002",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T19:05:00Z",
                "origin": {
                    "account_id": "ACC123456789",
                    "currency": "MXN",
                    "amount": 750.00
                },
                "destination": {
                    "account_id": "ACC987654321",
                    "currency": "USD",
                    "amount": 37.50
                }
            }
            """;

        log.info("Sending multiple events to Kafka...");

        // Act
        kafkaTemplate.send(TOPIC, event1);
        kafkaTemplate.send(TOPIC, event2);

        // Assert
        await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(1000))
            .untilAsserted(() -> {
                // Verify both events were processed
                List<TransactionEntity> transactions = transactionJpaRepository.findAll();
                assertTrue(transactions.size() >= 4, "Should have at least 4 transactions (2 debit + 2 credit)");
                log.info("Found {} transactions in database", transactions.size());

                // Verify final balances
                AccountEntity originAccount = accountJpaRepository.findByAccountNumber(ORIGIN_ACCOUNT).orElseThrow();
                assertTrue(originAccount.getBalance().compareTo(new BigDecimal("198750.00")) <= 0);
                log.info("Origin account final balance: {}", originAccount.getBalance());

                AccountEntity destinationAccount = accountJpaRepository.findByAccountNumber(DESTINATION_ACCOUNT).orElseThrow();
                assertTrue(destinationAccount.getBalance().compareTo(new BigDecimal("62.50")) >= 0);
                log.info("Destination account final balance: {}", destinationAccount.getBalance());
            });

        log.info("Multiple events test completed successfully!");
    }
}

