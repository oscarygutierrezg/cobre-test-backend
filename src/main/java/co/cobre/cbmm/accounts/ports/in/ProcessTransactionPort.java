package co.cobre.cbmm.accounts.ports.in;

import co.cobre.cbmm.accounts.domain.model.Transaction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Driving port for processing transactions
 */
public interface ProcessTransactionPort {

    /**
     * Process a credit transaction (add to balance)
     * @param accountId the account ID
     * @param amount the amount to credit
     * @return the created transaction
     */
    Transaction processCredit(UUID accountId, BigDecimal amount);

    /**
     * Process a debit transaction (subtract from balance)
     * @param accountId the account ID
     * @param amount the amount to debit
     * @return the created transaction
     */
    Transaction processDebit(UUID accountId, BigDecimal amount);
}


