package co.cobre.cbmm.accounts.ports.out;

import co.cobre.cbmm.accounts.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Driven port for transaction repository operations
 */
public interface TransactionRepositoryPort {

    /**
     * Save a transaction
     * @param transaction the transaction to save
     * @return the saved transaction
     */
    Transaction save(Transaction transaction);

    /**
     * Find all transactions for an account with pagination
     * @param accountId the account ID
     * @param pageable pagination information
     * @return page of transactions
     */
    Page<Transaction> findByAccountIdPaginated(UUID accountId, Pageable pageable);
}

