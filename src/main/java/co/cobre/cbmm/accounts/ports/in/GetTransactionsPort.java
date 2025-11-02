package co.cobre.cbmm.accounts.ports.in;

import co.cobre.cbmm.accounts.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Driving port for querying transactions
 */
public interface GetTransactionsPort {

    /**
     * Get all transactions for an account with pagination
     *
     * @param accountId the account ID
     * @param pageable pagination information
     * @return page of transactions ordered by creation date descending
     */
    Page<Transaction> getTransactionsByAccountId(UUID accountId, Pageable pageable);
}

