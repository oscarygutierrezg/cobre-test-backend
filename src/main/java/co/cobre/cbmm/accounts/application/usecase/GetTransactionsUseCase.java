package co.cobre.cbmm.accounts.application.usecase;

import co.cobre.cbmm.accounts.domain.model.Transaction;
import co.cobre.cbmm.accounts.ports.in.GetTransactionsPort;
import co.cobre.cbmm.accounts.ports.out.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for retrieving transactions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetTransactionsUseCase implements GetTransactionsPort {

    private final TransactionRepositoryPort transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionsByAccountId(UUID accountId, Pageable pageable) {
        log.info("Retrieving transactions for account: {}, page: {}, size: {}",
            accountId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Transaction> transactions = transactionRepository.findByAccountIdPaginated(accountId, pageable);

        log.info("Found {} transactions for account: {} (total: {})",
            transactions.getNumberOfElements(), accountId, transactions.getTotalElements());

        return transactions;
    }
}

