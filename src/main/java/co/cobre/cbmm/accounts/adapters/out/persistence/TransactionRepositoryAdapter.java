package co.cobre.cbmm.accounts.adapters.out.persistence;

import co.cobre.cbmm.accounts.adapters.out.persistence.entity.TransactionEntity;
import co.cobre.cbmm.accounts.adapters.out.persistence.repository.AccountJpaRepository;
import co.cobre.cbmm.accounts.adapters.out.persistence.repository.TransactionJpaRepository;
import co.cobre.cbmm.accounts.domain.exception.AccountNotFoundException;
import co.cobre.cbmm.accounts.domain.model.Transaction;
import co.cobre.cbmm.accounts.domain.model.TransactionStatus;
import co.cobre.cbmm.accounts.domain.model.TransactionType;
import co.cobre.cbmm.accounts.ports.out.TransactionRepositoryPort;
import co.cobre.cbmm.accounts.domain.model.Currency;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * JPA adapter implementing the TransactionRepositoryPort (Driven Adapter)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository transactionJpaRepository;
    private final AccountJpaRepository accountJpaRepository;

    @Override
    public Transaction save(Transaction transaction) {
        log.debug("Saving transaction: {}", transaction.transactionId());

        TransactionEntity entity = mapToEntity(transaction);
        TransactionEntity savedEntity = transactionJpaRepository.save(entity);

        log.info("Transaction saved successfully: {}", savedEntity.getTransactionId());
        return mapToDomain(savedEntity);
    }

    @Override
    public Page<Transaction> findByAccountIdPaginated(UUID accountId, Pageable pageable) {
        log.debug("Finding transactions for account {} with pagination: page={}, size={}",
            accountId, pageable.getPageNumber(), pageable.getPageSize());
        if (accountJpaRepository.findById(accountId).isEmpty()) {
            throw new AccountNotFoundException(accountId.toString());
        }


        Page<TransactionEntity> entityPage = transactionJpaRepository.findByAccountId(accountId, pageable);

        return entityPage.map(this::mapToDomain);
    }

    private Transaction mapToDomain(TransactionEntity entity) {
        return new Transaction(
            entity.getTransactionId(),
            entity.getAccountId(),
            entity.getAmount(),
            TransactionType.valueOf(entity.getType()),
            Currency.fromCode(entity.getCurrency()),
            entity.getBalanceAfter(),
            entity.getCreatedAt(),
            TransactionStatus.valueOf(entity.getStatus())
        );
    }

    private TransactionEntity mapToEntity(Transaction transaction) {
        return TransactionEntity.builder()
            // Don't set transactionId - let JPA generate it
            .accountId(transaction.accountId())
            .amount(transaction.amount())
            .type(transaction.type().name())
            .currency(transaction.currency().getCode())
            .balanceAfter(transaction.balanceAfter())
            .createdAt(transaction.createdAt())
            .status(transaction.status().name())
            .build();
    }
}

