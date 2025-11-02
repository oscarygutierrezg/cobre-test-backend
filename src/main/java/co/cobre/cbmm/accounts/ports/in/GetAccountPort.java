package co.cobre.cbmm.accounts.ports.in;

import co.cobre.cbmm.accounts.application.dto.AccountDTO;

/**
 * Driving port for getting account information
 */
public interface GetAccountPort {

    /**
     * Get account information by account number
     * @param accountNumber the account number
     * @return account information
     */
    AccountDTO getAccountByNumber(String accountNumber);
}

