package co.cobre.cbmm.accounts.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity for Account table
 */
@Entity
@Table(name = "account", schema = "cbmm")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "account_number", nullable = false, unique = true, length = 50)
    private String accountNumber;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (version == null) {
            version = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

