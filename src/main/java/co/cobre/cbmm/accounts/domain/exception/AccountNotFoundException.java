package co.cobre.cbmm.accounts.domain.exception;

/**
 * Exception thrown when an account is not found
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountNumber) {
        super("Account not found with number: " + accountNumber);
    }
}

