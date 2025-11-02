package co.cobre.cbmm.accounts.domain.model;

import co.cobre.cbmm.accounts.domain.model.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity representing a transaction
 */
public record Transaction(
    UUID transactionId,
    UUID accountId,
    BigDecimal amount,
    TransactionType type,
    Currency currency,
    BigDecimal balanceAfter,
    LocalDateTime createdAt,
    TransactionStatus status
) {

    public Transaction {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Amount must be non-zero");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (balanceAfter == null || balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance after must be non-negative");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    /**
     * Checks if the transaction is a debit (negative amount)
     */
    public boolean isDebit() {
        return TransactionType.DEBIT.equals(type);
    }

    /**
     * Checks if the transaction is a credit (positive amount)
     */
    public boolean isCredit() {
        return TransactionType.CREDIT.equals(type);
    }
}

