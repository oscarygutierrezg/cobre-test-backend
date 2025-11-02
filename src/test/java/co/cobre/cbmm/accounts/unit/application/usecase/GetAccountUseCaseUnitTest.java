package co.cobre.cbmm.accounts.unit.application.usecase;

import co.cobre.cbmm.accounts.application.dto.AccountDTO;
import co.cobre.cbmm.accounts.application.usecase.GetAccountUseCase;
import co.cobre.cbmm.accounts.domain.exception.AccountNotFoundException;
import co.cobre.cbmm.accounts.domain.model.Account;
import co.cobre.cbmm.accounts.domain.model.AccountStatus;
import co.cobre.cbmm.accounts.domain.model.Currency;
import co.cobre.cbmm.accounts.ports.out.AccountRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetAccountUseCaseUnitTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @InjectMocks
    private GetAccountUseCase getAccountUseCase;

    private Account testAccount;
    private String accountNumber;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        accountNumber = "ACC-2024-001";
        testAccount = new Account(
            accountId,
            accountNumber,
            Currency.USD,
            new BigDecimal("1000.00"),
            AccountStatus.ACTIVE,
            LocalDateTime.now(),
            LocalDateTime.now(),
            1
        );
    }

    @Nested
    @DisplayName("Get account by number tests")
    class GetAccountByNumberTests {

        @Test
        @DisplayName("Given valid account number, when getAccountByNumber, then return account DTO")
        void givenValidAccountNumber_whenGetAccountByNumber_thenReturnAccountDTO() {
            // Arrange
            when(accountRepositoryPort.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(testAccount));

            // Act
            AccountDTO result = getAccountUseCase.getAccountByNumber(accountNumber);

            // Assert
            assertNotNull(result);
            assertEquals(accountId, result.accountId());
            assertEquals(accountNumber, result.accountNumber());
            assertEquals("USD", result.currency());
            assertEquals(new BigDecimal("1000.00"), result.balance());
            assertEquals("ACTIVE", result.status());
            assertNotNull(result.createdAt());
            assertNotNull(result.updatedAt());
            assertEquals(1, result.version());

            verify(accountRepositoryPort).findByAccountNumber(accountNumber);
        }

        @Test
        @DisplayName("Given non-existent account number, when getAccountByNumber, then throw AccountNotFoundException")
        void givenNonExistentAccountNumber_whenGetAccountByNumber_thenThrowAccountNotFoundException() {
            // Arrange
            String nonExistentAccountNumber = "ACC-9999-999";
            when(accountRepositoryPort.findByAccountNumber(nonExistentAccountNumber))
                .thenReturn(Optional.empty());

            // Act & Assert
            AccountNotFoundException exception = assertThrows(
                AccountNotFoundException.class,
                () -> getAccountUseCase.getAccountByNumber(nonExistentAccountNumber)
            );

            assertTrue(exception.getMessage().contains(nonExistentAccountNumber));
            verify(accountRepositoryPort).findByAccountNumber(nonExistentAccountNumber);
        }

        @Test
        @DisplayName("Given account with zero balance, when getAccountByNumber, then return account DTO with zero balance")
        void givenAccountWithZeroBalance_whenGetAccountByNumber_thenReturnAccountDTOWithZeroBalance() {
            // Arrange
            Account zeroBalanceAccount = new Account(
                accountId,
                accountNumber,
                Currency.USD,
                BigDecimal.ZERO,
                AccountStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
            );
            when(accountRepositoryPort.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(zeroBalanceAccount));

            // Act
            AccountDTO result = getAccountUseCase.getAccountByNumber(accountNumber);

            // Assert
            assertNotNull(result);
            assertEquals(BigDecimal.ZERO, result.balance());
            verify(accountRepositoryPort).findByAccountNumber(accountNumber);
        }

        @Test
        @DisplayName("Given inactive account, when getAccountByNumber, then return account DTO with inactive status")
        void givenInactiveAccount_whenGetAccountByNumber_thenReturnAccountDTOWithInactiveStatus() {
            // Arrange
            Account inactiveAccount = new Account(
                accountId,
                accountNumber,
                Currency.USD,
                new BigDecimal("1000.00"),
                AccountStatus.INACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
            );
            when(accountRepositoryPort.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(inactiveAccount));

            // Act
            AccountDTO result = getAccountUseCase.getAccountByNumber(accountNumber);

            // Assert
            assertNotNull(result);
            assertEquals("INACTIVE", result.status());
            verify(accountRepositoryPort).findByAccountNumber(accountNumber);
        }
    }
}

