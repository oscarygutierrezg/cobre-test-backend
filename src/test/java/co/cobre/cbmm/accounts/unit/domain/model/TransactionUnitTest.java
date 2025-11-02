package co.cobre.cbmm.accounts.unit.domain.model;

import co.cobre.cbmm.accounts.domain.model.Currency;
import co.cobre.cbmm.accounts.domain.model.Transaction;
import co.cobre.cbmm.accounts.domain.model.TransactionStatus;
import co.cobre.cbmm.accounts.domain.model.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionUnitTest {

    @Nested
    @DisplayName("Transaction creation tests")
    class TransactionCreationTests {

        @Test
        @DisplayName("Given valid parameters, when creating transaction, then transaction is created successfully")
        void givenValidParameters_whenCreatingTransaction_thenTransactionIsCreatedSuccessfully() {
            // Arrange & Act
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionType.CREDIT,
                Currency.USD,
                new BigDecimal("1100.00"),
                LocalDateTime.now(),
                TransactionStatus.COMPLETED
            );

            // Assert
            assertNotNull(transaction);
            assertEquals(new BigDecimal("100.00"), transaction.amount());
            assertEquals(TransactionType.CREDIT, transaction.type());
            assertEquals(Currency.USD, transaction.currency());
            assertEquals(new BigDecimal("1100.00"), transaction.balanceAfter());
            assertEquals(TransactionStatus.COMPLETED, transaction.status());
        }

        @Test
        @DisplayName("Given null account ID, when creating transaction, then throw IllegalArgumentException")
        void givenNullAccountId_whenCreatingTransaction_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                    UUID.randomUUID(),
                    null,
                    new BigDecimal("100.00"),
                    TransactionType.CREDIT,
                    Currency.USD,
                    new BigDecimal("1100.00"),
                    LocalDateTime.now(),
                    TransactionStatus.COMPLETED
                )
            );
        }

        @Test
        @DisplayName("Given zero amount, when creating transaction, then throw IllegalArgumentException")
        void givenZeroAmount_whenCreatingTransaction_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    BigDecimal.ZERO,
                    TransactionType.CREDIT,
                    Currency.USD,
                    new BigDecimal("1000.00"),
                    LocalDateTime.now(),
                    TransactionStatus.COMPLETED
                )
            );
        }

        @Test
        @DisplayName("Given null amount, when creating transaction, then throw IllegalArgumentException")
        void givenNullAmount_whenCreatingTransaction_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    TransactionType.CREDIT,
                    Currency.USD,
                    new BigDecimal("1000.00"),
                    LocalDateTime.now(),
                    TransactionStatus.COMPLETED
                )
            );
        }

        @Test
        @DisplayName("Given null transaction type, when creating transaction, then throw IllegalArgumentException")
        void givenNullTransactionType_whenCreatingTransaction_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new BigDecimal("100.00"),
                    null,
                    Currency.USD,
                    new BigDecimal("1100.00"),
                    LocalDateTime.now(),
                    TransactionStatus.COMPLETED
                )
            );
        }

        @Test
        @DisplayName("Given null currency, when creating transaction, then throw IllegalArgumentException")
        void givenNullCurrency_whenCreatingTransaction_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new BigDecimal("100.00"),
                    TransactionType.CREDIT,
                    null,
                    new BigDecimal("1100.00"),
                    LocalDateTime.now(),
                    TransactionStatus.COMPLETED
                )
            );
        }

        @Test
        @DisplayName("Given negative balance after, when creating transaction, then throw IllegalArgumentException")
        void givenNegativeBalanceAfter_whenCreatingTransaction_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new BigDecimal("100.00"),
                    TransactionType.CREDIT,
                    Currency.USD,
                    new BigDecimal("-100.00"),
                    LocalDateTime.now(),
                    TransactionStatus.COMPLETED
                )
            );
        }

        @Test
        @DisplayName("Given null status, when creating transaction, then throw IllegalArgumentException")
        void givenNullStatus_whenCreatingTransaction_thenThrowIllegalArgumentException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    new BigDecimal("100.00"),
                    TransactionType.CREDIT,
                    Currency.USD,
                    new BigDecimal("1100.00"),
                    LocalDateTime.now(),
                    null
                )
            );
        }

        @Test
        @DisplayName("Given zero balance after, when creating transaction, then transaction is created")
        void givenZeroBalanceAfter_whenCreatingTransaction_thenTransactionIsCreated() {
            // Arrange & Act
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionType.DEBIT,
                Currency.USD,
                BigDecimal.ZERO,
                LocalDateTime.now(),
                TransactionStatus.COMPLETED
            );

            // Assert
            assertNotNull(transaction);
            assertEquals(BigDecimal.ZERO, transaction.balanceAfter());
        }
    }

    @Nested
    @DisplayName("Transaction behavior tests")
    class TransactionBehaviorTests {

        @Test
        @DisplayName("Given debit transaction, when isDebit, then return true")
        void givenDebitTransaction_whenIsDebit_thenReturnTrue() {
            // Arrange
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionType.DEBIT,
                Currency.USD,
                new BigDecimal("900.00"),
                LocalDateTime.now(),
                TransactionStatus.COMPLETED
            );

            // Act & Assert
            assertTrue(transaction.isDebit());
        }

        @Test
        @DisplayName("Given credit transaction, when isDebit, then return false")
        void givenCreditTransaction_whenIsDebit_thenReturnFalse() {
            // Arrange
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionType.CREDIT,
                Currency.USD,
                new BigDecimal("1100.00"),
                LocalDateTime.now(),
                TransactionStatus.COMPLETED
            );

            // Act & Assert
            assertFalse(transaction.isDebit());
        }

        @Test
        @DisplayName("Given credit transaction, when isCredit, then return true")
        void givenCreditTransaction_whenIsCredit_thenReturnTrue() {
            // Arrange
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionType.CREDIT,
                Currency.USD,
                new BigDecimal("1100.00"),
                LocalDateTime.now(),
                TransactionStatus.COMPLETED
            );

            // Act & Assert
            assertTrue(transaction.isCredit());
        }

        @Test
        @DisplayName("Given debit transaction, when isCredit, then return false")
        void givenDebitTransaction_whenIsCredit_thenReturnFalse() {
            // Arrange
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionType.DEBIT,
                Currency.USD,
                new BigDecimal("900.00"),
                LocalDateTime.now(),
                TransactionStatus.COMPLETED
            );

            // Act & Assert
            assertFalse(transaction.isCredit());
        }

        @Test
        @DisplayName("Given pending transaction, when status is PENDING, then verify status")
        void givenPendingTransaction_whenStatusIsPending_thenVerifyStatus() {
            // Arrange
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionType.CREDIT,
                Currency.USD,
                new BigDecimal("1100.00"),
                LocalDateTime.now(),
                TransactionStatus.PENDING
            );

            // Act & Assert
            assertEquals(TransactionStatus.PENDING, transaction.status());
        }

        @Test
        @DisplayName("Given failed transaction, when status is FAILED, then verify status")
        void givenFailedTransaction_whenStatusIsFailed_thenVerifyStatus() {
            // Arrange
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                TransactionType.CREDIT,
                Currency.USD,
                new BigDecimal("1000.00"),
                LocalDateTime.now(),
                TransactionStatus.FAILED
            );

            // Act & Assert
            assertEquals(TransactionStatus.FAILED, transaction.status());
        }
    }
}

