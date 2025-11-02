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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MsAccountsApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountControllerFunctionalTest extends BaseContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

    private final Faker faker = new Faker();
    private UUID createdAccountId;
    private String createdAccountNumber;


    @BeforeEach
    void setUp() {
        transactionJpaRepository.deleteAll();
        accountJpaRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should perform complete account lifecycle with create, retrieve, and query operations")
    void shouldPerformCompleteAccountLifecycle() throws Exception {
        // STEP 1: Create a new account
        createdAccountNumber = "ACC-" + faker.number().digits(8);
        AccountEntity newAccount = new AccountEntity();
        newAccount.setAccountNumber(createdAccountNumber);
        newAccount.setCurrency(Currency.USD.getCode());
        newAccount.setBalance(new BigDecimal("5000.00"));
        newAccount.setStatus(AccountStatus.ACTIVE.name());

        newAccount = accountJpaRepository.save(newAccount);
        createdAccountId = newAccount.getAccountId();
        log.info("Account created with ID: {} and number: {}", createdAccountId, createdAccountNumber);

        // STEP 2: Query the created account by account number
        MvcResult getResult = mockMvc.perform(get("/api/v1/accounts/{accountNumber}", createdAccountNumber))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account_number").value(createdAccountNumber))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.balance").value(5000.00))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andReturn();

        log.info("Account queried successfully by account number");

        // STEP 3: Create transactions for the account
        TransactionEntity creditTransaction = createTransaction(
            createdAccountId,
            new BigDecimal("1000.00"),
            TransactionType.CREDIT,
            new BigDecimal("6000.00")
        );
        transactionJpaRepository.save(creditTransaction);
        log.info("Credit transaction created");

        TransactionEntity debitTransaction = createTransaction(
            createdAccountId,
            new BigDecimal("500.00"),
            TransactionType.DEBIT,
            new BigDecimal("5500.00")
        );
        transactionJpaRepository.save(debitTransaction);
        log.info("Debit transaction created");

        // STEP 4: Query transactions for the account
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", createdAccountId)
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.total_elements").value(2));

        log.info("Transactions queried successfully");

        // STEP 5: Query transactions with pagination
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", createdAccountId)
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.total_elements").value(2))
            .andExpect(jsonPath("$.total_pages").value(2));

        log.info("Paginated transactions queried successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Should validate error cases and return appropriate HTTP status codes")
    void shouldHandleErrorScenarios() throws Exception {
        // STEP 1: Try to query non-existent account
        String nonExistentAccountNumber = "ACC-NONEXISTENT";
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", nonExistentAccountNumber))
            .andExpect(status().isNotFound());

        log.info("Non-existent account query returned 404 as expected");

        // STEP 2: Try to query transactions for non-existent account
        UUID nonExistentAccountId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", nonExistentAccountId))
            .andExpect(status().isNotFound());

        log.info("Non-existent account transactions query returned 404 as expected");

        // STEP 3: Try to query transactions with invalid page number
        AccountEntity testAccount = createAndSaveAccount();

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", testAccount.getAccountId())
                .param("page", "-1"))
            .andExpect(status().isBadRequest());

        log.info("Invalid page number returned 400 as expected");

        // STEP 4: Try to query transactions with invalid page size
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", testAccount.getAccountId())
                .param("size", "0"))
            .andExpect(status().isBadRequest());

        log.info("Invalid page size returned 400 as expected");
    }

    @Test
    @Order(3)
    @DisplayName("Should handle multiple accounts and transactions correctly")
    void shouldHandleMultipleAccountsAndTransactions() throws Exception {
        // STEP 1: Create multiple accounts
        AccountEntity account1 = createAndSaveAccount();
        AccountEntity account2 = createAndSaveAccount();
        AccountEntity account3 = createAndSaveAccount();

        log.info("Created 3 accounts: {}, {}, {}",
            account1.getAccountNumber(), account2.getAccountNumber(), account3.getAccountNumber());

        // STEP 2: Create transactions for each account
        // Account 1: 5 transactions
        for (int i = 0; i < 5; i++) {
            TransactionEntity transaction = createTransaction(
                account1.getAccountId(),
                new BigDecimal(String.valueOf((i + 1) * 100)),
                i % 2 == 0 ? TransactionType.CREDIT : TransactionType.DEBIT,
                new BigDecimal("1000.00")
            );
            transactionJpaRepository.save(transaction);
        }

        // Account 2: 3 transactions
        for (int i = 0; i < 3; i++) {
            TransactionEntity transaction = createTransaction(
                account2.getAccountId(),
                new BigDecimal(String.valueOf((i + 1) * 50)),
                TransactionType.CREDIT,
                new BigDecimal("1000.00")
            );
            transactionJpaRepository.save(transaction);
        }

        // Account 3: No transactions
        log.info("Created transactions for accounts");

        // STEP 3: Verify each account returns correct number of transactions
        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", account1.getAccountId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.total_elements").value(5));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", account2.getAccountId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(3)))
            .andExpect(jsonPath("$.total_elements").value(3));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", account3.getAccountId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.total_elements").value(0));

        log.info("All accounts returned correct transaction counts");

        // STEP 4: Verify each account can be queried by account number
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", account1.getAccountNumber()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account_number").value(account1.getAccountNumber()));

        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", account2.getAccountNumber()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account_number").value(account2.getAccountNumber()));

        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", account3.getAccountNumber()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account_number").value(account3.getAccountNumber()));

        log.info("All accounts queried successfully by account number");
    }

    @Test
    @Order(4)
    @DisplayName("Should handle inactive accounts correctly")
    void shouldHandleInactiveAccountsCorrectly() throws Exception {
        // STEP 1: Create active account
        AccountEntity activeAccount = createAndSaveAccount();

        // STEP 2: Create inactive account
        AccountEntity inactiveAccount = new AccountEntity();
        inactiveAccount.setAccountNumber("ACC-" + faker.number().digits(8));
        inactiveAccount.setCurrency(Currency.USD.getCode());
        inactiveAccount.setBalance(new BigDecimal("1000.00"));
        inactiveAccount.setStatus(AccountStatus.INACTIVE.name());
        inactiveAccount = accountJpaRepository.save(inactiveAccount);

        log.info("Created active and inactive accounts");

        // STEP 3: Query active account
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", activeAccount.getAccountNumber()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        // STEP 4: Query inactive account
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", inactiveAccount.getAccountNumber()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));

        log.info("Both active and inactive accounts queried successfully with correct status");
    }

    protected AccountEntity createAndSaveAccount() {
        AccountEntity account = new AccountEntity();
        account.setAccountNumber("ACC-" + faker.number().digits(8));
        account.setCurrency(Currency.USD.getCode());
        account.setBalance(new BigDecimal(faker.number().numberBetween(1000, 10000)));
        account.setStatus(AccountStatus.ACTIVE.name());
        return accountJpaRepository.save(account);
    }

    private TransactionEntity createTransaction(UUID accountId, BigDecimal amount, TransactionType type, BigDecimal balanceAfter) {
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

