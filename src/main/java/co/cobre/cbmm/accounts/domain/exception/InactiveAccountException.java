package co.cobre.cbmm.accounts.domain.exception;

import lombok.Getter;

/**
 * Exception thrown when trying to process a transaction on an inactive account
 */
@Getter
public class InactiveAccountException extends RuntimeException {

    private final String accountNumber;

    public InactiveAccountException(String accountNumber) {
        super(String.format("Account %s is not active", accountNumber));
        this.accountNumber = accountNumber;
    }
}

