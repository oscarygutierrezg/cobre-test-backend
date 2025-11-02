package co.cobre.cbmm.accounts.adapters.out.persistence.repository;

import co.cobre.cbmm.accounts.adapters.out.persistence.entity.CBMMEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for CBMM Event persistence
 */
@Repository
public interface CBMMEventJpaRepository extends JpaRepository<CBMMEventEntity, String> {

    /**
     * Find event by event ID
     */
    Optional<CBMMEventEntity> findByEventId(String eventId);

    /**
     * Find events by status
     */
    List<CBMMEventEntity> findByStatus(CBMMEventEntity.EventStatus status);

    /**
     * Find events by status and retry count less than max
     */
    @Query("SELECT e FROM CBMMEventEntity e WHERE e.status = :status AND e.retryCount < :maxRetries")
    List<CBMMEventEntity> findRetryableEvents(
        @Param("status") CBMMEventEntity.EventStatus status,
        @Param("maxRetries") Integer maxRetries
    );

    /**
     * Find events by operation date range
     */
    List<CBMMEventEntity> findByOperationDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Check if event exists by event ID
     */
    boolean existsByEventId(String eventId);

    /**
     * Count events by status
     */
    long countByStatus(CBMMEventEntity.EventStatus status);
}

