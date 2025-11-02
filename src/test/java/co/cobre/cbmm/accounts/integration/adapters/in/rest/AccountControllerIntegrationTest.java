package co.cobre.cbmm.accounts.integration.adapters.in.rest;

import co.cobre.cbmm.accounts.MsAccountsApplication;
import co.cobre.cbmm.accounts.adapters.out.persistence.entity.AccountEntity;
import co.cobre.cbmm.accounts.adapters.out.persistence.repository.AccountJpaRepository;
import co.cobre.cbmm.accounts.base.BaseContainerTest;
import co.cobre.cbmm.accounts.domain.model.AccountStatus;
import co.cobre.cbmm.accounts.domain.model.Currency;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MsAccountsApplication.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ActiveProfiles("test")
class AccountControllerIntegrationTest extends BaseContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    private AccountEntity testAccount;


    @BeforeEach
    void setUp() {
        accountJpaRepository.deleteAll();

        // Create test account
        testAccount = new AccountEntity();
        testAccount.setAccountNumber("ACC-2024-001");
        testAccount.setCurrency(Currency.USD.getCode());
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setStatus(AccountStatus.ACTIVE.name());

        testAccount = accountJpaRepository.save(testAccount);
    }

    @Test
    @DisplayName("Given valid account number in database, when getAccountByNumber, then return account")
    void givenValidAccountNumberInDatabase_whenGetAccountByNumber_thenReturnAccount() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", testAccount.getAccountNumber()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.account_number").value("ACC-2024-001"))
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.balance").value(1000.00))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Given non-existent account number, when getAccountByNumber, then return 404")
    void givenNonExistentAccountNumber_whenGetAccountByNumber_thenReturn404() throws Exception {
        // Arrange
        String nonExistentAccountNumber = "ACC-9999-999";

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", nonExistentAccountNumber))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Given inactive account in database, when getAccountByNumber, then return inactive account")
    void givenInactiveAccountInDatabase_whenGetAccountByNumber_thenReturnInactiveAccount() throws Exception {
        // Arrange - Create inactive account
        AccountEntity inactiveAccount = new AccountEntity();
        inactiveAccount.setAccountNumber("ACC-2024-002");
        inactiveAccount.setCurrency(Currency.USD.getCode());
        inactiveAccount.setBalance(new BigDecimal("500.00"));
        inactiveAccount.setStatus(AccountStatus.INACTIVE.name());

        inactiveAccount = accountJpaRepository.save(inactiveAccount);

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", inactiveAccount.getAccountNumber()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("Given account with zero balance in database, when getAccountByNumber, then return account with zero balance")
    void givenAccountWithZeroBalanceInDatabase_whenGetAccountByNumber_thenReturnAccountWithZeroBalance() throws Exception {
        // Arrange - Update test account to have zero balance
        testAccount.setBalance(BigDecimal.ZERO);
        testAccount = accountJpaRepository.save(testAccount);

        // Act & Assert
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", testAccount.getAccountNumber()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    @DisplayName("Given multiple accounts in database, when getAccountByNumber, then return correct account")
    void givenMultipleAccountsInDatabase_whenGetAccountByNumber_thenReturnCorrectAccount() throws Exception {
        // Arrange - Create additional accounts
        AccountEntity account2 = new AccountEntity();
        account2.setAccountNumber("ACC-2024-003");
        account2.setCurrency(Currency.EUR.getCode());
        account2.setBalance(new BigDecimal("2000.00"));
        account2.setStatus(AccountStatus.ACTIVE.name());

        accountJpaRepository.save(account2);

        // Act & Assert - Query first account
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", testAccount.getAccountNumber()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account_number").value("ACC-2024-001"))
            .andExpect(jsonPath("$.currency").value("USD"));

        // Act & Assert - Query second account
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", account2.getAccountNumber()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.account_number").value("ACC-2024-003"))
            .andExpect(jsonPath("$.currency").value("EUR"));
    }
}

