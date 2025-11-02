package co.cobre.cbmm.accounts.unit.adapters.out.persistence;

import co.cobre.cbmm.accounts.adapters.out.persistence.AccountRepositoryAdapter;
import co.cobre.cbmm.accounts.adapters.out.persistence.entity.AccountEntity;
import co.cobre.cbmm.accounts.adapters.out.persistence.repository.AccountJpaRepository;
import co.cobre.cbmm.accounts.domain.model.Account;
import co.cobre.cbmm.accounts.domain.model.AccountStatus;
import co.cobre.cbmm.accounts.domain.model.Currency;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountRepositoryAdapterUnitTest {

    @Mock
    private AccountJpaRepository accountJpaRepository;

    @InjectMocks
    private AccountRepositoryAdapter accountRepositoryAdapter;

    private AccountEntity accountEntity;
    private Account account;
    private UUID accountId;
    private String accountNumber;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        accountNumber = "ACC-2024-001";

        accountEntity = new AccountEntity();
        accountEntity.setAccountId(accountId);
        accountEntity.setAccountNumber(accountNumber);
        accountEntity.setCurrency(Currency.USD.getCode());
        accountEntity.setBalance(new BigDecimal("1000.00"));
        accountEntity.setStatus(AccountStatus.ACTIVE.name());
        accountEntity.setCreatedAt(LocalDateTime.now());
        accountEntity.setUpdatedAt(LocalDateTime.now());
        accountEntity.setVersion(1);

        account = new Account(
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
    @DisplayName("Find by account number tests")
    class FindByAccountNumberTests {

        @Test
        @DisplayName("Given existing account number, when findByAccountNumber, then return account")
        void givenExistingAccountNumber_whenFindByAccountNumber_thenReturnAccount() {
            // Arrange
            when(accountJpaRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(accountEntity));

            // Act
            Optional<Account> result = accountRepositoryAdapter.findByAccountNumber(accountNumber);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(accountNumber, result.get().accountNumber());
            assertEquals(Currency.USD, result.get().currency());
            assertEquals(new BigDecimal("1000.00"), result.get().balance());
            verify(accountJpaRepository).findByAccountNumber(accountNumber);
        }

        @Test
        @DisplayName("Given non-existing account number, when findByAccountNumber, then return empty")
        void givenNonExistingAccountNumber_whenFindByAccountNumber_thenReturnEmpty() {
            // Arrange
            String nonExistentNumber = "ACC-9999-999";
            when(accountJpaRepository.findByAccountNumber(nonExistentNumber))
                .thenReturn(Optional.empty());

            // Act
            Optional<Account> result = accountRepositoryAdapter.findByAccountNumber(nonExistentNumber);

            // Assert
            assertFalse(result.isPresent());
            verify(accountJpaRepository).findByAccountNumber(nonExistentNumber);
        }
    }

    @Nested
    @DisplayName("Find by ID tests")
    class FindByIdTests {

        @Test
        @DisplayName("Given existing account ID, when findById, then return account")
        void givenExistingAccountId_whenFindById_thenReturnAccount() {
            // Arrange
            when(accountJpaRepository.findById(accountId))
                .thenReturn(Optional.of(accountEntity));

            // Act
            Optional<Account> result = accountRepositoryAdapter.findById(accountId);

            // Assert
            assertTrue(result.isPresent());
            assertEquals(accountId, result.get().accountId());
            assertEquals(accountNumber, result.get().accountNumber());
            verify(accountJpaRepository).findById(accountId);
        }

        @Test
        @DisplayName("Given non-existing account ID, when findById, then return empty")
        void givenNonExistingAccountId_whenFindById_thenReturnEmpty() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(accountJpaRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

            // Act
            Optional<Account> result = accountRepositoryAdapter.findById(nonExistentId);

            // Assert
            assertFalse(result.isPresent());
            verify(accountJpaRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Save tests")
    class SaveTests {

        @Test
        @DisplayName("Given new account, when save, then create and return account")
        void givenNewAccount_whenSave_thenCreateAndReturnAccount() {
            // Arrange
            when(accountJpaRepository.findById(accountId))
                .thenReturn(Optional.empty());
            when(accountJpaRepository.save(any(AccountEntity.class)))
                .thenReturn(accountEntity);

            // Act
            Account result = accountRepositoryAdapter.save(account);

            // Assert
            assertNotNull(result);
            assertEquals(accountId, result.accountId());
            assertEquals(accountNumber, result.accountNumber());
            verify(accountJpaRepository).findById(accountId);
            verify(accountJpaRepository).save(any(AccountEntity.class));
        }

        @Test
        @DisplayName("Given existing account, when save, then update and return account")
        void givenExistingAccount_whenSave_thenUpdateAndReturnAccount() {
            // Arrange
            Account updatedAccount = new Account(
                accountId,
                accountNumber,
                Currency.USD,
                new BigDecimal("2000.00"),
                AccountStatus.ACTIVE,
                accountEntity.getCreatedAt(),
                LocalDateTime.now(),
                1
            );

            AccountEntity updatedEntity = new AccountEntity();
            updatedEntity.setAccountId(accountId);
            updatedEntity.setAccountNumber(accountNumber);
            updatedEntity.setCurrency(Currency.USD.getCode());
            updatedEntity.setBalance(new BigDecimal("2000.00"));
            updatedEntity.setStatus(AccountStatus.ACTIVE.name());
            updatedEntity.setCreatedAt(accountEntity.getCreatedAt());
            updatedEntity.setUpdatedAt(LocalDateTime.now());
            updatedEntity.setVersion(2);

            when(accountJpaRepository.findById(accountId))
                .thenReturn(Optional.of(accountEntity));
            when(accountJpaRepository.save(any(AccountEntity.class)))
                .thenReturn(updatedEntity);

            // Act
            Account result = accountRepositoryAdapter.save(updatedAccount);

            // Assert
            assertNotNull(result);
            assertEquals(new BigDecimal("2000.00"), result.balance());
            verify(accountJpaRepository).findById(accountId);
            verify(accountJpaRepository).save(any(AccountEntity.class));
        }

        @Test
        @DisplayName("Given account with different currency, when save, then save with new currency")
        void givenAccountWithDifferentCurrency_whenSave_thenSaveWithNewCurrency() {
            // Arrange
            Account eurAccount = new Account(
                accountId,
                accountNumber,
                Currency.EUR,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE,
                accountEntity.getCreatedAt(),
                LocalDateTime.now(),
                1
            );

            AccountEntity eurEntity = new AccountEntity();
            eurEntity.setAccountId(accountId);
            eurEntity.setAccountNumber(accountNumber);
            eurEntity.setCurrency(Currency.EUR.getCode());
            eurEntity.setBalance(new BigDecimal("1000.00"));
            eurEntity.setStatus(AccountStatus.ACTIVE.name());
            eurEntity.setCreatedAt(accountEntity.getCreatedAt());
            eurEntity.setUpdatedAt(LocalDateTime.now());
            eurEntity.setVersion(1);

            when(accountJpaRepository.findById(accountId))
                .thenReturn(Optional.of(accountEntity));
            when(accountJpaRepository.save(any(AccountEntity.class)))
                .thenReturn(eurEntity);

            // Act
            Account result = accountRepositoryAdapter.save(eurAccount);

            // Assert
            assertNotNull(result);
            assertEquals(Currency.EUR, result.currency());
            verify(accountJpaRepository).save(any(AccountEntity.class));
        }

        @Test
        @DisplayName("Given inactive account, when save, then save with inactive status")
        void givenInactiveAccount_whenSave_thenSaveWithInactiveStatus() {
            // Arrange
            Account inactiveAccount = new Account(
                accountId,
                accountNumber,
                Currency.USD,
                new BigDecimal("1000.00"),
                AccountStatus.INACTIVE,
                accountEntity.getCreatedAt(),
                LocalDateTime.now(),
                1
            );

            AccountEntity inactiveEntity = new AccountEntity();
            inactiveEntity.setAccountId(accountId);
            inactiveEntity.setAccountNumber(accountNumber);
            inactiveEntity.setCurrency(Currency.USD.getCode());
            inactiveEntity.setBalance(new BigDecimal("1000.00"));
            inactiveEntity.setStatus(AccountStatus.INACTIVE.name());
            inactiveEntity.setCreatedAt(accountEntity.getCreatedAt());
            inactiveEntity.setUpdatedAt(LocalDateTime.now());
            inactiveEntity.setVersion(1);

            when(accountJpaRepository.findById(accountId))
                .thenReturn(Optional.of(accountEntity));
            when(accountJpaRepository.save(any(AccountEntity.class)))
                .thenReturn(inactiveEntity);

            // Act
            Account result = accountRepositoryAdapter.save(inactiveAccount);

            // Assert
            assertNotNull(result);
            assertEquals(AccountStatus.INACTIVE, result.status());
            verify(accountJpaRepository).save(any(AccountEntity.class));
        }
    }
}

