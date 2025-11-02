package co.cobre.cbmm.accounts.application.usecase;

import co.cobre.cbmm.accounts.application.dto.AccountDTO;
import co.cobre.cbmm.accounts.domain.exception.AccountNotFoundException;
import co.cobre.cbmm.accounts.domain.model.Account;
import co.cobre.cbmm.accounts.ports.in.GetAccountPort;
import co.cobre.cbmm.accounts.ports.out.AccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for getting account information
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GetAccountUseCase implements GetAccountPort {

    private final AccountRepositoryPort accountRepositoryPort;

    @Override
    public AccountDTO getAccountByNumber(String accountNumber) {
        log.info("Getting account information for account number: {}", accountNumber);

        Account account = accountRepositoryPort.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        log.info("Account found: {}", account.accountNumber());

        return mapToDTO(account);
    }

    private AccountDTO mapToDTO(Account account) {
        return new AccountDTO(
            account.accountId(),
            account.accountNumber(),
            account.currency().getCode(),
            account.balance(),
            account.status().name(),
            account.createdAt(),
            account.updatedAt(),
            account.version()
        );
    }
}

