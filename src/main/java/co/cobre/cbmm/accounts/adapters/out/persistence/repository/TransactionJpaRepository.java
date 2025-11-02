package co.cobre.cbmm.accounts.adapters.out.persistence.repository;

import co.cobre.cbmm.accounts.adapters.out.persistence.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for Transaction entities
 */
@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {

    /**
     * Find all transactions for a specific account with pagination
     * @param accountId the account ID
     * @param pageable pagination information
     * @return page of transactions
     */
    Page<TransactionEntity> findByAccountId(UUID accountId, Pageable pageable);
}

