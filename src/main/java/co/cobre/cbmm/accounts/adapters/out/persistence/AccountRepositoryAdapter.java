package co.cobre.cbmm.accounts.adapters.out.persistence;

import co.cobre.cbmm.accounts.adapters.out.persistence.entity.AccountEntity;
import co.cobre.cbmm.accounts.adapters.out.persistence.repository.AccountJpaRepository;
import co.cobre.cbmm.accounts.domain.model.Account;
import co.cobre.cbmm.accounts.domain.model.AccountStatus;
import co.cobre.cbmm.accounts.domain.model.Currency;
import co.cobre.cbmm.accounts.ports.out.AccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing the AccountRepositoryPort (Driven Adapter)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository accountJpaRepository;

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        log.debug("Finding account by number: {}", accountNumber);

        return accountJpaRepository.findByAccountNumber(accountNumber)
            .map(this::mapToDomain);
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        log.debug("Finding account by ID: {}", accountId);

        return accountJpaRepository.findById(accountId)
            .map(this::mapToDomain);
    }

    @Override
    public Account save(Account account) {
        log.debug("Saving account: {}", account.accountNumber());

        // Find existing entity or create new one
        AccountEntity entity = accountJpaRepository.findById(account.accountId())
            .orElseGet(() -> {
                log.debug("Creating new account entity: {}", account.accountId());
                AccountEntity newEntity = new AccountEntity();
                newEntity.setAccountId(account.accountId());
                newEntity.setAccountNumber(account.accountNumber());
                newEntity.setCreatedAt(account.createdAt());
                return newEntity;
            });

        // Update entity with current values
        entity.setCurrency(account.currency().getCode());
        entity.setBalance(account.balance());
        entity.setStatus(account.status().name());
        entity.setUpdatedAt(account.updatedAt());
        // DO NOT set version manually - JPA handles it automatically with @Version

        AccountEntity savedEntity = accountJpaRepository.save(entity);

        log.info("Account saved successfully: {} - New balance: {}, Version: {}",
            savedEntity.getAccountNumber(), savedEntity.getBalance(), savedEntity.getVersion());
        return mapToDomain(savedEntity);
    }

    private Account mapToDomain(AccountEntity entity) {
        return new Account(
            entity.getAccountId(),
            entity.getAccountNumber(),
            Currency.fromCode(entity.getCurrency()),
            entity.getBalance(),
            AccountStatus.valueOf(entity.getStatus()),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }
}

