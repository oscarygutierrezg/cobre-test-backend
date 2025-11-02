package co.cobre.cbmm.accounts.integration.adapters.in.rest;

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
import com.fasterxml.jackson.databind.ObjectMapper;
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
@DisplayName("BatchEventController Integration Tests")
class BatchEventControllerIntegrationTest extends BaseContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

        // Create test accounts
        createTestAccount("ACC123456789", Currency.MXN, new BigDecimal("200000.00"));
        createTestAccount("ACC987654321", Currency.USD, BigDecimal.ZERO);
        createTestAccount("ACC111111111", Currency.COP, new BigDecimal("5000000.00"));
        createTestAccount("ACC222222222", Currency.EUR, BigDecimal.ZERO);

        log.info("Test accounts created successfully");
    }

    @Test
    @Order(1)
    @DisplayName("Given valid JSON array file, when uploadEventFile, then process all events successfully")
    void givenValidJsonArrayFile_whenUploadEventFile_thenProcessAllEventsSuccessfully() throws Exception {
        // Arrange
        String jsonContent = """
            [
                {
                    "event_id": "batch_test_001",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T15:32:10Z",
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
                },
                {
                    "event_id": "batch_test_002",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T16:00:00Z",
                    "origin": {
                        "account_id": "ACC111111111",
                        "currency": "COP",
                        "amount": 50000.00
                    },
                    "destination": {
                        "account_id": "ACC222222222",
                        "currency": "EUR",
                        "amount": 10.00
                    }
                }
            ]
            """;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "events.json",
            MediaType.APPLICATION_JSON_VALUE,
            jsonContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.batch_id").exists())
            .andExpect(jsonPath("$.total_events").value(2))
            .andExpect(jsonPath("$.successful_events").value(2))
            .andExpect(jsonPath("$.failed_events").value(0))
            .andExpect(jsonPath("$.results", hasSize(2)))
            .andExpect(jsonPath("$.results[0].event_id").value("batch_test_001"))
            .andExpect(jsonPath("$.results[0].success").value(true))
            .andExpect(jsonPath("$.results[1].event_id").value("batch_test_002"))
            .andExpect(jsonPath("$.results[1].success").value(true));

        // Verify events were persisted
        List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
        assertEquals(2, events.size());
        log.info("Verified {} events persisted in database", events.size());

        // Verify transactions were created
        List<TransactionEntity> transactions = transactionJpaRepository.findAll();
        assertEquals(4, transactions.size()); // 2 debit + 2 credit
        log.info("Verified {} transactions created", transactions.size());
    }

    @Test
    @Order(2)
    @DisplayName("Given JSONL file, when uploadEventFile, then process all events successfully")
    void givenJsonlFile_whenUploadEventFile_thenProcessAllEventsSuccessfully() throws Exception {
        // Arrange
        String jsonlContent = """
            {"event_id":"jsonl_test_001","event_type":"cross_border_money_movement","operation_date":"2025-09-09T15:32:10Z","origin":{"account_id":"ACC123456789","currency":"MXN","amount":500.00},"destination":{"account_id":"ACC987654321","currency":"USD","amount":25.00}}
            {"event_id":"jsonl_test_002","event_type":"cross_border_money_movement","operation_date":"2025-09-09T16:00:00Z","origin":{"account_id":"ACC111111111","currency":"COP","amount":25000.00},"destination":{"account_id":"ACC222222222","currency":"EUR","amount":5.00}}
            {"event_id":"jsonl_test_003","event_type":"cross_border_money_movement","operation_date":"2025-09-09T17:00:00Z","origin":{"account_id":"ACC123456789","currency":"MXN","amount":750.00},"destination":{"account_id":"ACC987654321","currency":"USD","amount":37.50}}
            """;

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "events.jsonl",
            "application/x-ndjson",
            jsonlContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.total_events").value(3))
            .andExpect(jsonPath("$.successful_events").value(3))
            .andExpect(jsonPath("$.failed_events").value(0))
            .andExpect(jsonPath("$.results", hasSize(3)));

        // Verify events were persisted
        List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
        assertEquals(3, events.size());
        log.info("Verified {} events persisted from JSONL file", events.size());
    }

    @Test
    @Order(3)
    @DisplayName("Given empty file, when uploadEventFile, then return 400")
    void givenEmptyFile_whenUploadEventFile_thenReturn400() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.json",
            MediaType.APPLICATION_JSON_VALUE,
            new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(emptyFile))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Invalid File"))
            .andExpect(jsonPath("$.message").value("File cannot be empty"));

        log.info("Empty file test completed successfully");
    }

    @Test
    @Order(4)
    @DisplayName("Given file with no valid events, when uploadEventFile, then return 400")
    void givenFileWithNoValidEvents_whenUploadEventFile_thenReturn400() throws Exception {
        // Arrange
        String emptyArrayContent = "[]";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "empty-array.json",
            MediaType.APPLICATION_JSON_VALUE,
            emptyArrayContent.getBytes(StandardCharsets.UTF_8)
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Empty Batch"))
            .andExpect(jsonPath("$.message").value("File contains no valid events"));

        log.info("No valid events test completed successfully");
    }

    @Test
    @Order(5)
    @DisplayName("Given file exceeding size limit, when uploadEventFile, then return 400")
    void givenFileExceedingSizeLimit_whenUploadEventFile_thenReturn400() throws Exception {
        // Arrange - Create a file larger than 10MB
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB

        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.json",
            MediaType.APPLICATION_JSON_VALUE,
            largeContent
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(largeFile))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("File Size Exceeded"))
            .andExpect(jsonPath("$.message").exists());

        log.info("File size exceeded test completed successfully");
    }

    @Test
    @Order(6)
    @DisplayName("Given file with mixed valid and invalid events, when uploadEventFile, then process valid events")
    void givenFileWithMixedValidAndInvalidEvents_whenUploadEventFile_thenProcessValidEvents() throws Exception {
        // Arrange
        String mixedContent = """
            [
                {
                    "event_id": "mixed_valid_001",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T15:32:10Z",
                    "origin": {
                        "account_id": "ACC123456789",
                        "currency": "MXN",
                        "amount": 300.00
                    },
                    "destination": {
                        "account_id": "ACC987654321",
                        "currency": "USD",
                        "amount": 15.00
                    }
                },
                {
                    "event_id": "mixed_invalid_001",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T16:00:00Z",
                    "origin": {
                        "account_id": "ACC_NOT_EXISTS",
                        "currency": "MXN",
                        "amount": 999999.00
                    },
                    "destination": {
                        "account_id": "ACC987654321",
                        "currency": "USD",
                        "amount": 50000.00
                    }
                },
                {
                    "event_id": "mixed_valid_002",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T17:00:00Z",
                    "origin": {
                        "account_id": "ACC111111111",
                        "currency": "COP",
                        "amount": 10000.00
                    },
                    "destination": {
                        "account_id": "ACC222222222",
                        "currency": "EUR",
                        "amount": 2.00
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

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_events").value(3))
            .andExpect(jsonPath("$.results", hasSize(3)))
            .andExpect(jsonPath("$.results[?(@.event_id == 'mixed_valid_001')].success").value(true))
            .andExpect(jsonPath("$.results[?(@.event_id == 'mixed_invalid_001')].success").value(false))
            .andExpect(jsonPath("$.results[?(@.event_id == 'mixed_valid_002')].success").value(true));

        // Verify only valid events were persisted
        List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
        assertEquals(2, events.size());
        log.info("Verified only {} valid events persisted from mixed file", events.size());
    }

    @Test
    @Order(7)
    @DisplayName("Given file with duplicate events, when uploadEventFile, then process each once")
    void givenFileWithDuplicateEvents_whenUploadEventFile_thenProcessEachOnce() throws Exception {
        // Arrange
        String duplicateContent = """
            [
                {
                    "event_id": "duplicate_test_001",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T15:32:10Z",
                    "origin": {
                        "account_id": "ACC123456789",
                        "currency": "MXN",
                        "amount": 200.00
                    },
                    "destination": {
                        "account_id": "ACC987654321",
                        "currency": "USD",
                        "amount": 10.00
                    }
                },
                {
                    "event_id": "duplicate_test_001",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T15:32:10Z",
                    "origin": {
                        "account_id": "ACC123456789",
                        "currency": "MXN",
                        "amount": 200.00
                    },
                    "destination": {
                        "account_id": "ACC987654321",
                        "currency": "USD",
                        "amount": 10.00
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

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.total_events").value(2))
            .andExpect(jsonPath("$.successful_events").value(1))
            .andExpect(jsonPath("$.failed_events").value(1));

        // Verify only one event was persisted
        List<CBMMEventEntity> events = cbmmEventJpaRepository.findAll();
        long count = events.stream()
            .filter(e -> e.getEventId().equals("duplicate_test_001"))
            .count();
        assertEquals(1, count, "Should have only one event with this ID");
        log.info("Verified duplicate event was processed only once");
    }

    @Test
    @Order(8)
    @DisplayName("Given invalid JSON format, when uploadEventFile, then return 400")
    void givenInvalidJsonFormat_whenUploadEventFile_thenReturn400() throws Exception {
        // Arrange
        String invalidJson = "{ this is not valid json }";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "invalid.json",
            MediaType.APPLICATION_JSON_VALUE,
            invalidJson.getBytes(StandardCharsets.UTF_8)
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400));

        log.info("Invalid JSON format test completed successfully");
    }

    @Test
    @Order(9)
    @DisplayName("Given large batch of valid events, when uploadEventFile, then process all concurrently")
    void givenLargeBatchOfValidEvents_whenUploadEventFile_thenProcessAllConcurrently() throws Exception {
        // Arrange - Create multiple valid events
        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 1; i <= 20; i++) {
            if (i > 1) jsonBuilder.append(",");
            jsonBuilder.append(String.format("""
                {
                    "event_id": "large_batch_%03d",
                    "event_type": "cross_border_money_movement",
                    "operation_date": "2025-09-09T15:32:10Z",
                    "origin": {
                        "account_id": "ACC123456789",
                        "currency": "MXN",
                        "amount": 100.00
                    },
                    "destination": {
                        "account_id": "ACC987654321",
                        "currency": "USD",
                        "amount": 5.00
                    }
                }
                """, i));
        }
        jsonBuilder.append("]");

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "large-batch.json",
            MediaType.APPLICATION_JSON_VALUE,
            jsonBuilder.toString().getBytes(StandardCharsets.UTF_8)
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/events/batch/upload")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.total_events").value(20))
            .andExpect(jsonPath("$.results", hasSize(20)));

        log.info("Large batch test completed successfully");
    }

    // Helper methods

    private void createTestAccount(String accountNumber, Currency currency, BigDecimal balance) {
        AccountEntity account = new AccountEntity();
        account.setAccountNumber(accountNumber);
        account.setCurrency(currency.getCode());
        account.setBalance(balance);
        account.setStatus(AccountStatus.ACTIVE.name());
        accountJpaRepository.save(account);
        log.debug("Created test account: {} with currency: {} and balance: {}",
            accountNumber, currency, balance);
    }
}

