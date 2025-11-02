package co.cobre.cbmm.accounts.application.dto;

import co.cobre.cbmm.accounts.domain.model.Transaction;
import co.cobre.cbmm.accounts.domain.model.TransactionStatus;
import co.cobre.cbmm.accounts.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for transaction responses
 */
public record TransactionResponseDTO(
    UUID transactionId,
    UUID accountId,
    BigDecimal amount,
    TransactionType type,
    String currency,
    BigDecimal balanceAfter,
    LocalDateTime createdAt,
    TransactionStatus status
) {
    public static TransactionResponseDTO from(Transaction transaction) {
        return new TransactionResponseDTO(
            transaction.transactionId(),
            transaction.accountId(),
            transaction.amount(),
            transaction.type(),
            transaction.currency().getCode(),
            transaction.balanceAfter(),
            transaction.createdAt(),
            transaction.status()
        );
    }
}

