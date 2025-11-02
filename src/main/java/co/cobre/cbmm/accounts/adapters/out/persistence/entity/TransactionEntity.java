package co.cobre.cbmm.accounts.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for Transaction table
 */
@Entity
@Table(name = "transaction", schema = "cbmm",
    indexes = {
        @Index(name = "idx_transaction_account_id", columnList = "account_id"),
        @Index(name = "idx_transaction_created_at", columnList = "created_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

