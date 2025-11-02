package co.cobre.cbmm.accounts.domain.model;

import co.cobre.cbmm.accounts.domain.model.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain Entity representing an account
 */
public record Account(
    UUID accountId,
    String accountNumber,
    Currency currency,
    BigDecimal balance,
    AccountStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Integer version
) {

    public Account {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be null or empty");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance must be non-negative");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
    }

    /**
     * Checks if the account is active
     */
    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(status);
    }

    /**
     * Creates a new Account with an updated balance
     */
    public Account withBalance(BigDecimal newBalance) {
        return new Account(
            accountId,
            accountNumber,
            currency,
            newBalance,
            status,
            createdAt,
            LocalDateTime.now(),
            version
        );
    }

    /**
     * Creates a new Account with an incremented version
     */
    public Account withIncrementedVersion() {
        return new Account(
            accountId,
            accountNumber,
            currency,
            balance,
            status,
            createdAt,
            updatedAt,
            version != null ? version + 1 : 1
        );
    }

    /**
     * Checks if account has sufficient balance for a debit
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
}

