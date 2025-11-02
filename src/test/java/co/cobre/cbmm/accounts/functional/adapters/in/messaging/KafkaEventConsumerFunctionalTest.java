package co.cobre.cbmm.accounts.functional.adapters.in.messaging;

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
import com.github.javafaker.Faker;
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
@DisplayName("KafkaEventConsumer Functional Tests - Complete CBMM Flow")
class KafkaEventConsumerFunctionalTest extends BaseContainerTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

    @Autowired
    private CBMMEventJpaRepository cbmmEventJpaRepository;

    private final Faker faker = new Faker();
    private static final String TOPIC = "cbmm-events-test";

    @BeforeEach
    void setUp() {
        log.info("Cleaning up database before test...");
        cbmmEventJpaRepository.deleteAll();
        transactionJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Scenario: Complete cross-border money movement from MXN to USD")
    void completeCrossBorderMoneyMovementFromMXNtoUSD() {
        // Given: Two active accounts with initial balances
        String originAccountNumber = "ACC-MXN-" + faker.number().digits(6);
        String destAccountNumber = "ACC-USD-" + faker.number().digits(6);

        AccountEntity originAccount = createAndSaveAccount(
            originAccountNumber,
            Currency.MXN,
            new BigDecimal("200000.00")
        );

        AccountEntity destAccount = createAndSaveAccount(
            destAccountNumber,
            Currency.USD,
            BigDecimal.ZERO
        );

        log.info("Created origin account: {} with balance: {}", originAccountNumber, originAccount.getBalance());
        log.info("Created destination account: {} with balance: {}", destAccountNumber, destAccount.getBalance());

        // When: A CBMM event is sent via Kafka
        String eventId = "cbmm_" + System.currentTimeMillis() + "_001";
        String eventJson = String.format("""
            {
                "event_id": "%s",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T15:32:10Z",
                "origin": {
                    "account_id": "%s",
                    "currency": "MXN",
                    "amount": 18000.00
                },
                "destination": {
                    "account_id": "%s",
                    "currency": "USD",
                    "amount": 1000.00
                }
            }
            """, eventId, originAccountNumber, destAccountNumber);

        kafkaTemplate.send(TOPIC, eventJson);
        log.info("Sent CBMM event: {}", eventId);

        // Then: The event is processed successfully
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                // Verify event is persisted and completed
                CBMMEventEntity persistedEvent = cbmmEventJpaRepository.findByEventId(eventId)
                    .orElseThrow(() -> new AssertionError("Event should be persisted"));
                assertEquals(CBMMEventEntity.EventStatus.COMPLETED, persistedEvent.getStatus());
                log.info("Event status: {}", persistedEvent.getStatus());

                // Verify origin account balance decreased
                AccountEntity updatedOriginAccount = accountJpaRepository.findByAccountNumber(originAccountNumber).orElseThrow();
                assertEquals(new BigDecimal("182000.00"), updatedOriginAccount.getBalance());
                log.info("Origin account new balance: {}", updatedOriginAccount.getBalance());

                // Verify destination account balance increased
                AccountEntity updatedDestAccount = accountJpaRepository.findByAccountNumber(destAccountNumber).orElseThrow();
                assertEquals(new BigDecimal("1000.00"), updatedDestAccount.getBalance());
                log.info("Destination account new balance: {}", updatedDestAccount.getBalance());

                // Verify transactions are created
                List<TransactionEntity> transactions = transactionJpaRepository.findAll();
                assertEquals(2, transactions.size());

                // Verify debit transaction
                TransactionEntity debitTx = transactions.stream()
                    .filter(tx -> tx.getType().equals(TransactionType.DEBIT.name()))
                    .findFirst()
                    .orElseThrow();
                assertEquals(new BigDecimal("18000.00"), debitTx.getAmount());
                assertEquals(TransactionStatus.COMPLETED.name(), debitTx.getStatus());
                log.info("Debit transaction: {} {}", debitTx.getAmount(), debitTx.getCurrency());

                // Verify credit transaction
                TransactionEntity creditTx = transactions.stream()
                    .filter(tx -> tx.getType().equals(TransactionType.CREDIT.name()))
                    .findFirst()
                    .orElseThrow();
                assertEquals(new BigDecimal("1000.00"), creditTx.getAmount());
                assertEquals(TransactionStatus.COMPLETED.name(), creditTx.getStatus());
                log.info("Credit transaction: {} {}", creditTx.getAmount(), creditTx.getCurrency());
            });

        log.info("Test completed successfully!");
    }

    @Test
    @Order(2)
    @DisplayName("Scenario: Duplicate event idempotency ensures single processing")
    void duplicateEventIdempotencyEnsuresSingleProcessing() throws InterruptedException {
        // Given: Active accounts
        String originAccountNumber = "ACC-COP-" + faker.number().digits(6);
        String destAccountNumber = "ACC-EUR-" + faker.number().digits(6);

        createAndSaveAccount(originAccountNumber, Currency.COP, new BigDecimal("5000000.00"));
        createAndSaveAccount(destAccountNumber, Currency.EUR, BigDecimal.ZERO);

        // When: Same event is sent multiple times
        String eventId = "cbmm_duplicate_" + System.currentTimeMillis();
        String eventJson = String.format("""
            {
                "event_id": "%s",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T16:00:00Z",
                "origin": {
                    "account_id": "%s",
                    "currency": "COP",
                    "amount": 50000.00
                },
                "destination": {
                    "account_id": "%s",
                    "currency": "EUR",
                    "amount": 10.00
                }
            }
            """, eventId, originAccountNumber, destAccountNumber);

        // Send event 3 times
        kafkaTemplate.send(TOPIC, eventJson);
        Thread.sleep(2000);
        kafkaTemplate.send(TOPIC, eventJson);
        Thread.sleep(2000);
        kafkaTemplate.send(TOPIC, eventJson);

        log.info("Sent duplicate event 3 times: {}", eventId);

        // Then: Event is processed only once
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                // Verify only one event record
                List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
                long eventCount = events.stream()
                    .filter(e -> e.getEventId().equals(eventId))
                    .count();
                assertEquals(1, eventCount, "Should have only one event record");

                // Verify only 2 transactions (1 debit + 1 credit)
                List<TransactionEntity> transactions = transactionJpaRepository.findAll();
                assertTrue(transactions.size() >= 2, "Should have at least 2 transactions");

                // Verify balances reflect single processing
                AccountEntity originAccount = accountJpaRepository.findByAccountNumber(originAccountNumber).orElseThrow();
                assertEquals(new BigDecimal("4950000.00"), originAccount.getBalance());

                AccountEntity destAccount = accountJpaRepository.findByAccountNumber(destAccountNumber).orElseThrow();
                assertEquals(new BigDecimal("10.00"), destAccount.getBalance());
            });

        log.info("Idempotency test completed successfully!");
    }

    @Test
    @Order(3)
    @DisplayName("Scenario: Insufficient balance prevents transaction")
    void insufficientBalancePreventsTransaction() throws InterruptedException {
        // Given: Account with low balance
        String originAccountNumber = "ACC-LOW-" + faker.number().digits(6);
        String destAccountNumber = "ACC-HIGH-" + faker.number().digits(6);

        createAndSaveAccount(originAccountNumber, Currency.USD, new BigDecimal("100.00"));
        createAndSaveAccount(destAccountNumber, Currency.EUR, BigDecimal.ZERO);

        BigDecimal originBalanceBefore = accountJpaRepository.findByAccountNumber(originAccountNumber)
            .orElseThrow()
            .getBalance();

        // When: Event with amount exceeding balance is sent
        String eventId = "cbmm_insufficient_" + System.currentTimeMillis();
        String eventJson = String.format("""
            {
                "event_id": "%s",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T17:00:00Z",
                "origin": {
                    "account_id": "%s",
                    "currency": "USD",
                    "amount": 500.00
                },
                "destination": {
                    "account_id": "%s",
                    "currency": "EUR",
                    "amount": 450.00
                }
            }
            """, eventId, originAccountNumber, destAccountNumber);

        kafkaTemplate.send(TOPIC, eventJson);
        log.info("Sent event with insufficient balance: {}", eventId);

        // Then: Transaction is rejected
        Thread.sleep(5000);

        // Verify balances remain unchanged
        AccountEntity originAccount = accountJpaRepository.findByAccountNumber(originAccountNumber).orElseThrow();
        assertEquals(originBalanceBefore, originAccount.getBalance());

        AccountEntity destAccount = accountJpaRepository.findByAccountNumber(destAccountNumber).orElseThrow();
        assertEquals(new BigDecimal("4500.00"), destAccount.getBalance());

        log.info("Insufficient balance test completed successfully!");
    }

    @Test
    @Order(4)
    @DisplayName("Scenario: Multiple concurrent events are processed correctly")
    void multipleConcurrentEventsAreProcessedCorrectly() {
        // Given: Accounts with sufficient balance
        String originAccountNumber = "ACC-MULTI-" + faker.number().digits(6);
        String destAccountNumber = "ACC-DEST-" + faker.number().digits(6);

        createAndSaveAccount(originAccountNumber, Currency.MXN, new BigDecimal("100000.00"));
        createAndSaveAccount(destAccountNumber, Currency.USD, BigDecimal.ZERO);

        // When: Multiple events are sent concurrently
        String eventId1 = "cbmm_concurrent_" + System.currentTimeMillis() + "_1";
        String eventId2 = "cbmm_concurrent_" + System.currentTimeMillis() + "_2";
        String eventId3 = "cbmm_concurrent_" + System.currentTimeMillis() + "_3";

        String event1Json = createEventJson(eventId1, originAccountNumber, destAccountNumber, "10000.00", "500.00");
        String event2Json = createEventJson(eventId2, originAccountNumber, destAccountNumber, "5000.00", "250.00");
        String event3Json = createEventJson(eventId3, originAccountNumber, destAccountNumber, "3000.00", "150.00");

        // Send all events
        kafkaTemplate.send(TOPIC, event1Json);
        kafkaTemplate.send(TOPIC, event2Json);
        kafkaTemplate.send(TOPIC, event3Json);

        log.info("Sent 3 concurrent events");

        // Then: All events are processed successfully
        await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(1000))
            .untilAsserted(() -> {
                // Verify all 3 events are completed
                List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
                long completedEvents = events.stream()
                    .filter(e -> e.getStatus().equals(CBMMEventEntity.EventStatus.COMPLETED))
                    .count();
                assertTrue(completedEvents >= 3, "Should have at least 3 completed events");

                // Verify all 6 transactions (3 debit + 3 credit)
                List<TransactionEntity> transactions = transactionJpaRepository.findAll();
                assertTrue(transactions.size() >= 6, "Should have at least 6 transactions");

                // Verify final balances
                AccountEntity originAccount = accountJpaRepository.findByAccountNumber(originAccountNumber).orElseThrow();
                assertEquals(new BigDecimal("82000.00"), originAccount.getBalance());

                AccountEntity destAccount = accountJpaRepository.findByAccountNumber(destAccountNumber).orElseThrow();
                assertEquals(new BigDecimal("900.00"), destAccount.getBalance());
            });

        log.info("Concurrent events test completed successfully!");
    }

    @Test
    @Order(5)
    @DisplayName("Scenario: Invalid account prevents transaction")
    void invalidAccountPreventsTransaction() throws InterruptedException {
        // Given: Only destination account exists
        String destAccountNumber = "ACC-VALID-" + faker.number().digits(6);
        createAndSaveAccount(destAccountNumber, Currency.USD, BigDecimal.ZERO);

        // When: Event with non-existent origin account is sent
        String eventId = "cbmm_invalid_" + System.currentTimeMillis();
        String eventJson = String.format("""
            {
                "event_id": "%s",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T18:00:00Z",
                "origin": {
                    "account_id": "ACC-NOT-EXISTS",
                    "currency": "MXN",
                    "amount": 1000.00
                },
                "destination": {
                    "account_id": "%s",
                    "currency": "USD",
                    "amount": 50.00
                }
            }
            """, eventId, destAccountNumber);

        kafkaTemplate.send(TOPIC, eventJson);
        log.info("Sent event with invalid origin account: {}", eventId);

        // Then: Transaction is rejected
        Thread.sleep(5000);

        // Verify destination balance unchanged
        AccountEntity destAccount = accountJpaRepository.findByAccountNumber(destAccountNumber).orElseThrow();
        assertEquals(new BigDecimal("500.00"), destAccount.getBalance());

        // Verify no transactions created for this event
        List<TransactionEntity> transactions = transactionJpaRepository.findAll();
        assertEquals(10, transactions.size(), "No transactions should be created");

        log.info("Invalid account test completed successfully!");
    }

    // Helper methods

    private AccountEntity createAndSaveAccount(String accountNumber, Currency currency, BigDecimal balance) {
        AccountEntity account = new AccountEntity();
        account.setAccountNumber(accountNumber);
        account.setCurrency(currency.getCode());
        account.setBalance(balance);
        account.setStatus(AccountStatus.ACTIVE.name());
        return accountJpaRepository.save(account);
    }

    private String createEventJson(String eventId, String originAccount, String destAccount,
                                   String originAmount, String destAmount) {
        return String.format("""
            {
                "event_id": "%s",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T15:00:00Z",
                "origin": {
                    "account_id": "%s",
                    "currency": "MXN",
                    "amount": %s
                },
                "destination": {
                    "account_id": "%s",
                    "currency": "USD",
                    "amount": %s
                }
            }
            """, eventId, originAccount, originAmount, destAccount, destAmount);
    }
}

