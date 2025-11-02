package co.cobre.cbmm.accounts.integration.adapters.in.rest;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MsAccountsApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ActiveProfiles("test")
class TransactionControllerIntegrationTest extends BaseContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

    private AccountEntity testAccount;
    private UUID accountId;


    @BeforeEach
    void setUp() {
        transactionJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();

        // Create test account
        testAccount = new AccountEntity();
        testAccount.setAccountNumber("ACC-2024-001");
        testAccount.setCurrency(Currency.USD.getCode());
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setStatus(AccountStatus.ACTIVE.name());

        testAccount = accountJpaRepository.save(testAccount);
        accountId = testAccount.getAccountId();
    }

    @Test
    @DisplayName("Given account with transactions, when getTransactions, then return paginated transactions")
    void givenAccountWithTransactions_whenGetTransactions_thenReturnPaginatedTransactions() throws Exception {
        // Arrange - Create transactions
        TransactionEntity transaction1 = createTransaction(
            new BigDecimal("100.00"),
            TransactionType.CREDIT,
            new BigDecimal("1100.00")
        );

        TransactionEntity transaction2 = createTransaction(
            new BigDecimal("50.00"),
            TransactionType.DEBIT,
            new BigDecimal("1050.00")
        );

        transactionJpaRepository.save(transaction1);
        transactionJpaRepository.save(transaction2);

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.total_elements").value(2))
            .andExpect(jsonPath("$.total_pages").value(1));
    }

    @Test
    @DisplayName("Given account with no transactions, when getTransactions, then return empty page")
    void givenAccountWithNoTransactions_whenGetTransactions_thenReturnEmptyPage() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.total_elements").value(0));
    }

    @Test
    @DisplayName("Given non-existent account ID, when getTransactions, then return 404")
    void givenNonExistentAccountId_whenGetTransactions_thenReturn404() throws Exception {
        // Arrange
        UUID nonExistentAccountId = UUID.randomUUID();

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", nonExistentAccountId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Given multiple pages of transactions, when getTransactions with pagination, then return correct page")
    void givenMultiplePagesOfTransactions_whenGetTransactionsWithPagination_thenReturnCorrectPage() throws Exception {
        // Arrange - Create 25 transactions
        for (int i = 0; i < 25; i++) {
            TransactionEntity transaction = createTransaction(
                new BigDecimal(String.valueOf(i + 1)),
                TransactionType.CREDIT,
                new BigDecimal("1000.00").add(new BigDecimal(String.valueOf(i + 1)))
            );
            transactionJpaRepository.save(transaction);
        }

        // Act & Assert - First page
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(10)))
            .andExpect(jsonPath("$.total_elements").value(25))
            .andExpect(jsonPath("$.total_pages").value(3))
            .andExpect(jsonPath("$.page_number").value(0));

        // Act & Assert - Second page
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(10)))
            .andExpect(jsonPath("$.total_elements").value(25))
            .andExpect(jsonPath("$.page_number").value(1));

        // Act & Assert - Third page
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("page", "2")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.total_elements").value(25))
            .andExpect(jsonPath("$.page_number").value(2));
    }

    @Test
    @DisplayName("Given transactions with different types, when getTransactions, then return all types correctly")
    void givenTransactionsWithDifferentTypes_whenGetTransactions_thenReturnAllTypesCorrectly() throws Exception {
        // Arrange - Create credit and debit transactions
        TransactionEntity creditTransaction = createTransaction(
            new BigDecimal("200.00"),
            TransactionType.CREDIT,
            new BigDecimal("1200.00")
        );

        TransactionEntity debitTransaction = createTransaction(
            new BigDecimal("100.00"),
            TransactionType.DEBIT,
            new BigDecimal("1100.00")
        );

        transactionJpaRepository.save(creditTransaction);
        transactionJpaRepository.save(debitTransaction);

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("Given negative page number, when getTransactions, then return 400")
    void givenNegativePageNumber_whenGetTransactions_thenReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("page", "-1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Given invalid page size, when getTransactions, then return 400")
    void givenInvalidPageSize_whenGetTransactions_thenReturn400() throws Exception {
        // Act & Assert - size > 100
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("size", "101"))
            .andExpect(status().isBadRequest());

        // Act & Assert - size <= 0
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                .param("size", "0"))
            .andExpect(status().isBadRequest());
    }

    private TransactionEntity createTransaction(BigDecimal amount, TransactionType type, BigDecimal balanceAfter) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setType(type.name());
        transaction.setCurrency(Currency.USD.getCode());
        transaction.setBalanceAfter(balanceAfter);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setStatus(TransactionStatus.COMPLETED.name());
        return transaction;
    }
}

