package co.cobre.cbmm.accounts.functional.adapters.in.rest;

import co.cobre.cbmm.accounts.MsAccountsApplication;
import co.cobre.cbmm.accounts.adapters.out.persistence.entity.AccountEntity;
import co.cobre.cbmm.accounts.adapters.out.persistence.entity.TransactionEntity;
import co.cobre.cbmm.accounts.adapters.out.persistence.repository.AccountJpaRepository;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
@DisplayName("TransactionController Functional Tests - Complete Transaction History Flow")
class TransactionControllerFunctionalTest extends BaseContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

    private final Faker faker = new Faker();
    private UUID accountId;

    @BeforeEach
    void setUp() {
        log.info("Cleaning up database before test...");
        transactionJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();

        // Create test account
        AccountEntity testAccount = new AccountEntity();
        testAccount.setAccountNumber("ACC-FUNC-" + faker.number().digits(6));
        testAccount.setCurrency(Currency.USD.getCode());
        testAccount.setBalance(new BigDecimal("10000.00"));
        testAccount.setStatus(AccountStatus.ACTIVE.name());

        testAccount = accountJpaRepository.save(testAccount);
        accountId = testAccount.getAccountId();

        log.info("Created test account: {} with ID: {}", testAccount.getAccountNumber(), accountId);
    }

    @Test
    @Order(1)
    @DisplayName("Scenario: Retrieve transaction history with pagination")
    void retrieveTransactionHistoryWithPagination() throws Exception {
        // Given: Account with transaction history
        log.info("Creating transaction history for account: {}", accountId);
        createTransactionHistory(accountId, 25);

        // When: Request first page
        log.info("Requesting first page of transactions");
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("page", "0")
                .param("size", "10"))
            // Then: Return first 10 transactions
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(10)))
            .andExpect(jsonPath("$.total_elements").value(25))
            .andExpect(jsonPath("$.total_pages").value(3))
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(10))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false));

        log.info("First page retrieved successfully");

        // When: Request second page
        log.info("Requesting second page of transactions");
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("page", "1")
                .param("size", "10"))
            // Then: Return next 10 transactions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(10)))
            .andExpect(jsonPath("$.page_number").value(1))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(false));

        log.info("Second page retrieved successfully");

        // When: Request last page
        log.info("Requesting last page of transactions");
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("page", "2")
                .param("size", "10"))
            // Then: Return remaining 5 transactions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.page_number").value(2))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true));

        log.info("Pagination test completed successfully!");
    }

    @Test
    @Order(2)
    @DisplayName("Scenario: Transactions are ordered by creation date descending")
    void transactionsAreOrderedByCreationDateDescending() throws Exception {
        // Given: Account with transactions created at different times
        log.info("Creating transactions with different timestamps");

        LocalDateTime baseTime = LocalDateTime.now().minusDays(5);

        for (int i = 0; i < 5; i++) {
            TransactionEntity transaction = createTransaction(
                accountId,
                new BigDecimal(String.valueOf(i + 1)),
                TransactionType.CREDIT,
                new BigDecimal("10000.00").add(new BigDecimal(String.valueOf(i + 1)))
            );
            transaction.setCreatedAt(baseTime.plusDays(i));
            transactionJpaRepository.save(transaction);
            log.debug("Created transaction {} at {}", i+1, transaction.getCreatedAt());
        }

        // When: Request transactions
        log.info("Requesting transactions ordered by date");
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId))
            // Then: Transactions are ordered newest first
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.content[0].amount").value(5.00)) // Most recent
            .andExpect(jsonPath("$.content[4].amount").value(1.00)); // Oldest

        log.info("Ordering test completed successfully!");
    }

    @Test
    @Order(3)
    @DisplayName("Scenario: Handle empty transaction history")
    void handleEmptyTransactionHistory() throws Exception {
        // Given: Account with no transactions (setUp already cleans DB)
        log.info("Testing empty transaction history for account: {}", accountId);

        // When: Request transactions
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId))
            // Then: Return empty page
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.total_elements").value(0))
            .andExpect(jsonPath("$.total_pages").value(0))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));

        log.info("Empty history test completed successfully!");
    }

    @Test
    @Order(4)
    @DisplayName("Scenario: Return 404 for non-existent account")
    void return404ForNonExistentAccount() throws Exception {
        // Given: Non-existent account ID
        UUID nonExistentAccountId = UUID.randomUUID();
        log.info("Testing non-existent account: {}", nonExistentAccountId);

        // When: Request transactions
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", nonExistentAccountId))
            // Then: Return 404
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Account Not Found"));

        log.info("Non-existent account test completed successfully!");
    }

    @Test
    @Order(5)
    @DisplayName("Scenario: View complete transaction details")
    void viewCompleteTransactionDetails() throws Exception {
        // Given: Account with detailed transactions
        log.info("Creating transactions with complete details");

        TransactionEntity creditTx = createTransaction(
            accountId,
            new BigDecimal("1500.00"),
            TransactionType.CREDIT,
            new BigDecimal("11500.00")
        );
        transactionJpaRepository.save(creditTx);

        TransactionEntity debitTx = createTransaction(
            accountId,
            new BigDecimal("500.00"),
            TransactionType.DEBIT,
            new BigDecimal("11000.00")
        );
        transactionJpaRepository.save(debitTx);

        // When: Request transactions
        log.info("Requesting transaction details");
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId))
            // Then: All transaction details are included
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].transaction_id").exists())
            .andExpect(jsonPath("$.content[0].account_id").value(accountId.toString()))
            .andExpect(jsonPath("$.content[0].amount").exists())
            //.andExpect(jsonPath("$.content[0].type").("CREDIT", "DEBIT"))
            .andExpect(jsonPath("$.content[0].currency").value("USD"))
            .andExpect(jsonPath("$.content[0].balance_after").exists())
            .andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
            .andExpect(jsonPath("$.content[0].created_at").exists());

        log.info("Transaction details test completed successfully!");
    }

    @Test
    @Order(6)
    @DisplayName("Scenario: Filter transactions by type (CREDIT and DEBIT)")
    void filterTransactionsByType() throws Exception {
        // Given: Account with mixed transaction types
        log.info("Creating mixed transaction types");

        // Create 3 credits
        for (int i = 0; i < 3; i++) {
            createAndSaveTransaction(
                accountId,
                new BigDecimal("100.00"),
                TransactionType.CREDIT,
                new BigDecimal("10100.00").add(new BigDecimal(i * 100))
            );
        }

        // Create 2 debits
        for (int i = 0; i < 2; i++) {
            createAndSaveTransaction(
                accountId,
                new BigDecimal("50.00"),
                TransactionType.DEBIT,
                new BigDecimal("10050.00").subtract(new BigDecimal(i * 50))
            );
        }

        // When: Request all transactions
        log.info("Requesting all transactions");
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId))
            // Then: Both types are present
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.content[?(@.type == 'CREDIT')]", hasSize(3)))
            .andExpect(jsonPath("$.content[?(@.type == 'DEBIT')]", hasSize(2)));

        log.info("Transaction type filter test completed successfully!");
    }

    @Test
    @Order(7)
    @DisplayName("Scenario: Handle large transaction history efficiently")
    void handleLargeTransactionHistoryEfficiently() throws Exception {
        // Given: Account with large transaction history
        log.info("Creating large transaction history (100 transactions)");
        createTransactionHistory(accountId, 100);

        // When: Request with different page sizes
        log.info("Testing different page sizes");

        // Small page size
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.total_pages").value(20));

        // Medium page size
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("size", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(25)))
            .andExpect(jsonPath("$.total_pages").value(4));

        // Large page size
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(50)))
            .andExpect(jsonPath("$.total_pages").value(2));

        log.info("Large history test completed successfully!");
    }

    @Test
    @Order(8)
    @DisplayName("Scenario: Validate pagination constraints")
    void validatePaginationConstraints() throws Exception {
        // Given: Account with transactions
        createTransactionHistory(accountId, 10);

        log.info("Testing pagination constraints");

        // When: Invalid page number (negative)
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("page", "-1"))
            // Then: Return 400
            .andExpect(status().isBadRequest());

        // When: Invalid page size (too large)
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("size", "101"))
            // Then: Return 400
            .andExpect(status().isBadRequest());

        // When: Invalid page size (zero)
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("size", "0"))
            // Then: Return 400
            .andExpect(status().isBadRequest());

        // When: Valid constraints
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("page", "0")
                .param("size", "20"))
            // Then: Return success
            .andExpect(status().isOk());

        log.info("Pagination constraints test completed successfully!");
    }

    @Test
    @Order(9)
    @DisplayName("Scenario: Multiple accounts with isolated transaction histories")
    void multipleAccountsWithIsolatedTransactionHistories() throws Exception {
        // Given: Multiple accounts with their own transactions
        log.info("Creating multiple accounts with isolated transactions");

        // Create second account
        AccountEntity account2 = new AccountEntity();
        account2.setAccountNumber("ACC-FUNC-" + faker.number().digits(6));
        account2.setCurrency(Currency.EUR.getCode());
        account2.setBalance(new BigDecimal("5000.00"));
        account2.setStatus(AccountStatus.ACTIVE.name());
        account2 = accountJpaRepository.save(account2);
        UUID account2Id = account2.getAccountId();

        // Create transactions for first account
        createTransactionHistory(accountId, 5);

        // Create transactions for second account
        createTransactionHistory(account2Id, 3);

        // When: Request transactions for first account
        log.info("Requesting transactions for account 1");
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId))
            // Then: Only first account's transactions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.content[*].account_id", everyItem(equalTo(accountId.toString()))));

        // When: Request transactions for second account
        log.info("Requesting transactions for account 2");
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", account2Id))
            // Then: Only second account's transactions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(3)))
            .andExpect(jsonPath("$.content[*].account_id", everyItem(equalTo(account2Id.toString()))));

        log.info("Multiple accounts isolation test completed successfully!");
    }

    // Helper methods

    private void createTransactionHistory(UUID accountId, int count) {
        List<TransactionEntity> transactions = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(count);

        for (int i = 0; i < count; i++) {
            TransactionType type = i % 2 == 0 ? TransactionType.CREDIT : TransactionType.DEBIT;
            BigDecimal amount = new BigDecimal(faker.number().numberBetween(10, 1000));
            BigDecimal balanceAfter = new BigDecimal("10000.00")
                .add(amount.multiply(type == TransactionType.CREDIT ? BigDecimal.ONE : BigDecimal.ONE.negate()));

            TransactionEntity transaction = createTransaction(accountId, amount, type, balanceAfter);
            transaction.setCreatedAt(baseTime.plusHours(i));
            transactions.add(transaction);
        }

        transactionJpaRepository.saveAll(transactions);
        log.debug("Created {} transactions for account {}", count, accountId);
    }

    private TransactionEntity createTransaction(UUID accountId, BigDecimal amount,
                                               TransactionType type, BigDecimal balanceAfter) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setType(type.name());
        transaction.setCurrency(Currency.USD.getCode());
        transaction.setBalanceAfter(balanceAfter);
        transaction.setStatus(TransactionStatus.COMPLETED.name());
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }

    private void createAndSaveTransaction(UUID accountId, BigDecimal amount,
                                         TransactionType type, BigDecimal balanceAfter) {
        TransactionEntity transaction = createTransaction(accountId, amount, type, balanceAfter);
        transactionJpaRepository.save(transaction);
    }
}

