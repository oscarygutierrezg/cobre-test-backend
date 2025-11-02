package co.cobre.cbmm.accounts.unit.domain.model;

import co.cobre.cbmm.accounts.domain.model.Account;
import co.cobre.cbmm.accounts.domain.model.AccountStatus;
import co.cobre.cbmm.accounts.domain.model.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountUnitTest {

    @Nested
    @DisplayName("Account creation tests")
    class AccountCreationTests {

        @Test
        @DisplayName("Given valid parameters, when creating account, then account is created successfully")
        void givenValidParameters_whenCreatingAccount_thenAccountIsCreatedSuccessfully() {
            // Arrange & Act
            Account account = new Account(
                UUID.randomUUID(),
                "ACC-2024-001",
                Currency.USD,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
            );

            // Assert
            assertNotNull(account);
            assertEquals("ACC-2024-001", account.accountNumber());
            assertEquals(Currency.USD, account.currency());
            assertEquals(new BigDecimal("1000.00"), account.balance());
            assertEquals(AccountStatus.ACTIVE, account.status());
        }

        @Test
        @DisplayName("Given null account number, when creating account, then throw IllegalArgumentException")
        void givenNullAccountNumber_whenCreatingAccount_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Account(
                    UUID.randomUUID(),
                    null,
                    Currency.USD,
                    new BigDecimal("1000.00"),
                    AccountStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    1
                )
            );
        }

        @Test
        @DisplayName("Given blank account number, when creating account, then throw IllegalArgumentException")
        void givenBlankAccountNumber_whenCreatingAccount_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Account(
                    UUID.randomUUID(),
                    "   ",
                    Currency.USD,
                    new BigDecimal("1000.00"),
                    AccountStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    1
                )
            );
        }

        @Test
        @DisplayName("Given null currency, when creating account, then throw IllegalArgumentException")
        void givenNullCurrency_whenCreatingAccount_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Account(
                    UUID.randomUUID(),
                    "ACC-2024-001",
                    null,
                    new BigDecimal("1000.00"),
                    AccountStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    1
                )
            );
        }

        @Test
        @DisplayName("Given negative balance, when creating account, then throw IllegalArgumentException")
        void givenNegativeBalance_whenCreatingAccount_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Account(
                    UUID.randomUUID(),
                    "ACC-2024-001",
                    Currency.USD,
                    new BigDecimal("-100.00"),
                    AccountStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    1
                )
            );
        }

        @Test
        @DisplayName("Given null status, when creating account, then throw IllegalArgumentException")
        void givenNullStatus_whenCreatingAccount_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Account(
                    UUID.randomUUID(),
                    "ACC-2024-001",
                    Currency.USD,
                    new BigDecimal("1000.00"),
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    1
                )
            );
        }

        @Test
        @DisplayName("Given zero balance, when creating account, then account is created")
        void givenZeroBalance_whenCreatingAccount_thenAccountIsCreated() {
            // Arrange & Act
            Account account = new Account(
                UUID.randomUUID(),
                "ACC-2024-001",
                Currency.USD,
                BigDecimal.ZERO,
                AccountStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
            );

            // Assert
            assertNotNull(account);
            assertEquals(BigDecimal.ZERO, account.balance());
        }
    }

    @Nested
    @DisplayName("Account behavior tests")
    class AccountBehaviorTests {

        @Test
        @DisplayName("Given active account, when isActive, then return true")
        void givenActiveAccount_whenIsActive_thenReturnTrue() {
            // Arrange
            Account account = new Account(
                UUID.randomUUID(),
                "ACC-2024-001",
                Currency.USD,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
            );

            // Act & Assert
            assertTrue(account.isActive());
        }

        @Test
        @DisplayName("Given inactive account, when isActive, then return false")
        void givenInactiveAccount_whenIsActive_thenReturnFalse() {
            // Arrange
            Account account = new Account(
                UUID.randomUUID(),
                "ACC-2024-001",
                Currency.USD,
                new BigDecimal("1000.00"),
                AccountStatus.INACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
            );

            // Act & Assert
            assertFalse(account.isActive());
        }

        @Test
        @DisplayName("Given account with balance, when withBalance, then return new account with new balance")
        void givenAccountWithBalance_whenWithBalance_thenReturnNewAccountWithNewBalance() {
            // Arrange
            Account originalAccount = new Account(
                UUID.randomUUID(),
                "ACC-2024-001",
                Currency.USD,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
            );

            // Act
            Account updatedAccount = originalAccount.withBalance(new BigDecimal("2000.00"));

            // Assert
            assertEquals(new BigDecimal("2000.00"), updatedAccount.balance());
            assertEquals(new BigDecimal("1000.00"), originalAccount.balance()); // Original unchanged
            assertEquals(originalAccount.accountNumber(), updatedAccount.accountNumber());
        }

        @Test
        @DisplayName("Given account, when withIncrementedVersion, then return new account with incremented version")
        void givenAccount_whenWithIncrementedVersion_thenReturnNewAccountWithIncrementedVersion() {
            // Arrange
            Account originalAccount = new Account(
                UUID.randomUUID(),
                "ACC-2024-001",
                Currency.USD,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
            );

            // Act
            Account updatedAccount = originalAccount.withIncrementedVersion();

            // Assert
            assertEquals(2, updatedAccount.version());
            assertEquals(1, originalAccount.version()); // Original unchanged
        }

        @Test
        @DisplayName("Given account with null version, when withIncrementedVersion, then return account with version 1")
        void givenAccountWithNullVersion_whenWithIncrementedVersion_thenReturnAccountWithVersion1() {
            // Arrange
            Account account = new Account(
                UUID.randomUUID(),
                "ACC-2024-001",
                Currency.USD,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
            );

            // Act
            Account updatedAccount = account.withIncrementedVersion();

            // Assert
            assertEquals(1, updatedAccount.version());
        }

        @Test
        @DisplayName("Given account with sufficient balance, when hasSufficientBalance, then return true")
        void givenAccountWithSufficientBalance_whenHasSufficientBalance_thenReturnTrue() {
            // Arrange
            Account account = new Account(
                UUID.randomUUID(),
                "ACC-2024-001",
                Currency.USD,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
            );

            // Act & Assert
            assertTrue(account.hasSufficientBalance(new BigDecimal("500.00")));
            assertTrue(account.hasSufficientBalance(new BigDecimal("1000.00")));
        }

        @Test
        @DisplayName("Given account with insufficient balance, when hasSufficientBalance, then return false")
        void givenAccountWithInsufficientBalance_whenHasSufficientBalance_thenReturnFalse() {
            // Arrange
            Account account = new Account(
                UUID.randomUUID(),
                "ACC-2024-001",
                Currency.USD,
                new BigDecimal("1000.00"),
                AccountStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                1
            );

            // Act & Assert
            assertFalse(account.hasSufficientBalance(new BigDecimal("1500.00")));
        }
    }
}

