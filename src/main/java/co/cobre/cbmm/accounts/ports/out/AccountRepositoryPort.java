package co.cobre.cbmm.accounts.ports.out;

import co.cobre.cbmm.accounts.domain.model.Account;

import java.util.Optional;
import java.util.UUID;

/**
 * Driven port for account repository operations
 */
public interface AccountRepositoryPort {

    /**
     * Find account by account number
     * @param accountNumber the account number
     * @return optional account
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find account by ID
     * @param accountId the account ID
     * @return optional account
     */
    Optional<Account> findById(UUID accountId);

    /**
     * Save or update an account
     * @param account the account to save
     * @return the saved account
     */
    Account save(Account account);
}

