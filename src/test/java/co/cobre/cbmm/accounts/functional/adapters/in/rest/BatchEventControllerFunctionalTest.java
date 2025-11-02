package co.cobre.cbmm.accounts.functional.adapters.in.rest;

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
import com.github.javafaker.Faker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest(
    classes = MsAccountsApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("BatchEventController Functional Tests - Complete Batch Processing Flow")
class BatchEventControllerFunctionalTest extends BaseContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

    @Autowired
    private CBMMEventJpaRepository cbmmEventJpaRepository;


    @BeforeEach
    void setUp() {
        log.info("Cleaning up database before test...");
        cbmmEventJpaRepository.deleteAll();
        transactionJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();

        // Create test accounts for all scenarios
        createTestAccounts();
        log.info("Test accounts created successfully");
    }

    @Test
    @Order(1)
    @DisplayName("Scenario: Upload batch file and process all events successfully")
    void uploadBatchFileAndProcessAllEventsSuccessfully() throws Exception {
        // Given: A JSON file with multiple valid CBMM events
        String jsonContent = """
            [
                {
                    "event_id": "batch_functional_001",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T15:32:10Z",
                    "origin": {
                        "account_id": "ACC-MXN-001",
                        "currency": "MXN",
                        "amount": 10000.00
                    },
                    "destination": {
                        "account_id": "ACC-USD-001",
                        "currency": "USD",
                        "amount": 500.00
                    }
                },
                {
                    "event_id": "batch_functional_002",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T16:00:00Z",
                    "origin": {
                        "account_id": "ACC-COP-001",
                        "currency": "COP",
                        "amount": 100000.00
                    },
                    "destination": {
                        "account_id": "ACC-EUR-001",
                        "currency": "EUR",
                        "amount": 20.00
                    }
                },
                {
                    "event_id": "batch_functional_003",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T17:00:00Z",
                    "origin": {
                        "account_id": "ACC-MXN-001",
                        "currency": "MXN",
                        "amount": 5000.00
                    },
                    "destination": {
                        "account_id": "ACC-USD-001",
                        "currency": "USD",
                        "amount": 250.00
                    }
                }
            ]
            """;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "cbmm-events.json",
            MediaType.APPLICATION_JSON_VALUE,
            jsonContent.getBytes(StandardCharsets.UTF_8)
        );

        log.info("Uploading batch file with 3 events");

        // When: The file is uploaded
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            // Then: All events are processed successfully
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.batch_id").exists())
            .andExpect(jsonPath("$.total_events").value(3))
            .andExpect(jsonPath("$.successful_events").value(3))
            .andExpect(jsonPath("$.failed_events").value(0))
            .andExpect(jsonPath("$.results", hasSize(3)))
            .andExpect(jsonPath("$.results[?(@.event_id == 'batch_functional_001')].success").value(true))
            .andExpect(jsonPath("$.results[?(@.event_id == 'batch_functional_002')].success").value(true))
            .andExpect(jsonPath("$.results[?(@.event_id == 'batch_functional_003')].success").value(true));

        // Verify database state
        List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
        assertEquals(3, events.size(), "All 3 events should be persisted");
        log.info("Verified {} events persisted", events.size());

        List<TransactionEntity> transactions = transactionJpaRepository.findAll();
        assertEquals(6, transactions.size(), "Should have 6 transactions (3 debit + 3 credit)");
        log.info("Verified {} transactions created", transactions.size());

        // Verify account balances updated correctly
        AccountEntity mxnAccount = accountJpaRepository.findByAccountNumber("ACC-MXN-001").orElseThrow();
        assertEquals(new BigDecimal("185000.00"), mxnAccount.getBalance());
        log.info("MXN account final balance: {}", mxnAccount.getBalance());

        AccountEntity usdAccount = accountJpaRepository.findByAccountNumber("ACC-USD-001").orElseThrow();
        assertEquals(new BigDecimal("750.00"), usdAccount.getBalance());
        log.info("USD account final balance: {}", usdAccount.getBalance());

        log.info("Batch upload test completed successfully!");
    }

    @Test
    @Order(2)
    @DisplayName("Scenario: Upload JSONL file and process line by line")
    void uploadJsonlFileAndProcessLineByLine() throws Exception {
        // Given: A JSONL file with events
        String jsonlContent = """
            {"event_id":"jsonl_func_001","event_type":"cross_border_money_movement","operation_date":"2025-09-09T15:32:10Z","origin":{"account_id":"ACC-MXN-001","currency":"MXN","amount":2000.00},"destination":{"account_id":"ACC-USD-001","currency":"USD","amount":100.00}}
            {"event_id":"jsonl_func_002","event_type":"cross_border_money_movement","operation_date":"2025-09-09T16:00:00Z","origin":{"account_id":"ACC-COP-001","currency":"COP","amount":50000.00},"destination":{"account_id":"ACC-EUR-001","currency":"EUR","amount":10.00}}
            """;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "events.jsonl",
            "application/x-ndjson",
            jsonlContent.getBytes(StandardCharsets.UTF_8)
        );

        log.info("Uploading JSONL file with 2 events");

        // When & Then
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_events").value(2))
            .andExpect(jsonPath("$.successful_events").value(2))
            .andExpect(jsonPath("$.failed_events").value(0));

        // Verify events persisted
        List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
        assertEquals(2, events.size());
        log.info("JSONL test completed successfully!");
    }

    @Test
    @Order(3)
    @DisplayName("Scenario: Handle mixed valid and invalid events gracefully")
    void handleMixedValidAndInvalidEventsGracefully() throws Exception {
        // Given: File with valid and invalid events
        String mixedContent = """
            [
                {
                    "event_id": "mixed_func_valid_001",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T15:32:10Z",
                    "origin": {
                        "account_id": "ACC-MXN-001",
                        "currency": "MXN",
                        "amount": 1000.00
                    },
                    "destination": {
                        "account_id": "ACC-USD-001",
                        "currency": "USD",
                        "amount": 50.00
                    }
                },
                {
                    "event_id": "mixed_func_invalid_001",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T16:00:00Z",
                    "origin": {
                        "account_id": "ACC-NONEXISTENT",
                        "currency": "MXN",
                        "amount": 999999.00
                    },
                    "destination": {
                        "account_id": "ACC-USD-001",
                        "currency": "USD",
                        "amount": 50000.00
                    }
                },
                {
                    "event_id": "mixed_func_valid_002",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T17:00:00Z",
                    "origin": {
                        "account_id": "ACC-COP-001",
                        "currency": "COP",
                        "amount": 25000.00
                    },
                    "destination": {
                        "account_id": "ACC-EUR-001",
                        "currency": "EUR",
                        "amount": 5.00
                    }
                }
            ]
            """;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "mixed-events.json",
            MediaType.APPLICATION_JSON_VALUE,
            mixedContent.getBytes(StandardCharsets.UTF_8)
        );

        log.info("Uploading file with mixed valid/invalid events");

        // When & Then
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_events").value(3))
            .andExpect(jsonPath("$.successful_events").value(2))
            .andExpect(jsonPath("$.failed_events").value(1))
            .andExpect(jsonPath("$.results[?(@.event_id == 'mixed_func_valid_001')].success").value(true))
            .andExpect(jsonPath("$.results[?(@.event_id == 'mixed_func_invalid_001')].success").value(false))
            .andExpect(jsonPath("$.results[?(@.event_id == 'mixed_func_valid_002')].success").value(true));

        // Verify only valid events persisted
        List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
        assertEquals(2, events.size(), "Only valid events should be persisted");
        log.info("Mixed events test completed successfully!");
    }

    @Test
    @Order(4)
    @DisplayName("Scenario: Reject file exceeding size limit")
    void rejectFileExceedingSizeLimit() throws Exception {
        // Given: File larger than 10MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        
        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "large-file.json",
            MediaType.APPLICATION_JSON_VALUE,
            largeContent
        );

        log.info("Uploading file exceeding size limit (11MB)");

        // When & Then
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(largeFile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("File Size Exceeded"));

        log.info("File size limit test completed successfully!");
    }

    @Test
    @Order(5)
    @DisplayName("Scenario: Duplicate events in batch are handled with idempotency")
    void duplicateEventsInBatchAreHandledWithIdempotency() throws Exception {
        // Given: File with duplicate events
        String duplicateContent = """
            [
                {
                    "event_id": "duplicate_batch_func_001",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T15:32:10Z",
                    "origin": {
                        "account_id": "ACC-MXN-001",
                        "currency": "MXN",
                        "amount": 500.00
                    },
                    "destination": {
                        "account_id": "ACC-USD-001",
                        "currency": "USD",
                        "amount": 25.00
                    }
                },
                {
                    "event_id": "duplicate_batch_func_001",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T15:32:10Z",
                    "origin": {
                        "account_id": "ACC-MXN-001",
                        "currency": "MXN",
                        "amount": 500.00
                    },
                    "destination": {
                        "account_id": "ACC-USD-001",
                        "currency": "USD",
                        "amount": 25.00
                    }
                }
            ]
            """;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "duplicate-events.json",
            MediaType.APPLICATION_JSON_VALUE,
            duplicateContent.getBytes(StandardCharsets.UTF_8)
        );

        log.info("Uploading file with duplicate events");

        // When & Then
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_events").value(2))
            .andExpect(jsonPath("$.successful_events").value(1))
            .andExpect(jsonPath("$.failed_events").value(1));

        // Verify only one event persisted
        List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
        long count = events.stream()
            .filter(e -> e.getEventId().equals("duplicate_batch_func_001"))
            .count();
        assertEquals(1, count, "Should have only one event with this ID");
        log.info("Duplicate events test completed successfully!");
    }

    // Helper methods

    private void createTestAccounts() {
        createAccount("ACC-MXN-001", Currency.MXN, new BigDecimal("200000.00"));
        createAccount("ACC-USD-001", Currency.USD, BigDecimal.ZERO);
        createAccount("ACC-COP-001", Currency.COP, new BigDecimal("5000000.00"));
        createAccount("ACC-EUR-001", Currency.EUR, BigDecimal.ZERO);
    }

    private void createAccount(String accountNumber, Currency currency, BigDecimal balance) {
        AccountEntity account = new AccountEntity();
        account.setAccountNumber(accountNumber);
        account.setCurrency(currency.getCode());
        account.setBalance(balance);
        account.setStatus(AccountStatus.ACTIVE.name());
        accountJpaRepository.save(account);
        log.debug("Created account: {} with currency: {} and balance: {}", 
            accountNumber, currency, balance);
    }
}

