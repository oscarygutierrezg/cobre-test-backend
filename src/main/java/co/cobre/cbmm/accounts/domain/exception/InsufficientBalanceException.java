package co.cobre.cbmm.accounts.domain.exception;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Exception thrown when an account has insufficient balance for a transaction
 */
@Getter
public class InsufficientBalanceException extends RuntimeException {

    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;
    private final String accountNumber;

    public InsufficientBalanceException(String accountNumber, BigDecimal currentBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient balance. Current: %s, Requested: %s", currentBalance, requestedAmount));
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
        this.accountNumber = accountNumber;
    }
}

