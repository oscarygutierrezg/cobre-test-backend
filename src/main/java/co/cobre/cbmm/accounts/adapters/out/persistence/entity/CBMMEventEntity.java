package co.cobre.cbmm.accounts.adapters.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.envers.Audited;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * JPA Entity for CBMM Event persistence
 * Stores Cross-Border Money Movement events with full audit trail
 */
@Entity
@Table(name = "cbmm_event", schema = "cbmm")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class CBMMEventEntity {

    @Id
    @Column(name = "event_id", length = 100, nullable = false)
    private String eventId;

    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;

    @Column(name = "operation_date", nullable = false)
    private ZonedDateTime operationDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "origin_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> originData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "destination_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> destinationData;

    @Column(name = "status", length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Event status enum
     */
    public enum EventStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        RETRYING
    }
}

