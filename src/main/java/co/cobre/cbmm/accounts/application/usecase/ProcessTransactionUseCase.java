package co.cobre.cbmm.accounts.application.usecase;

import co.cobre.cbmm.accounts.domain.exception.AccountNotFoundException;
import co.cobre.cbmm.accounts.domain.exception.InactiveAccountException;
import co.cobre.cbmm.accounts.domain.exception.InsufficientBalanceException;
import co.cobre.cbmm.accounts.domain.model.Account;
import co.cobre.cbmm.accounts.domain.model.Transaction;
import co.cobre.cbmm.accounts.domain.model.TransactionStatus;
import co.cobre.cbmm.accounts.domain.model.TransactionType;
import co.cobre.cbmm.accounts.domain.service.DistributedLockService;
import co.cobre.cbmm.accounts.ports.in.ProcessTransactionPort;
import co.cobre.cbmm.accounts.ports.out.AccountRepositoryPort;
import co.cobre.cbmm.accounts.ports.out.TransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Use case for processing transactions with distributed locks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessTransactionUseCase implements ProcessTransactionPort {

    private final AccountRepositoryPort accountRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final DistributedLockService distributedLockService;

    @Override
    @Transactional
    public Transaction processCredit(UUID accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }

        log.info("Processing credit transaction for account: {}, amount: {}", accountId, amount);

        return distributedLockService.executeWithLock(accountId.toString(), () -> {
            // 1. Find account
            Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

            // 2. Validate account is active
            if (!account.isActive()) {
                throw new InactiveAccountException(account.accountNumber());
            }

            // 3. Calculate new balance
            BigDecimal newBalance = account.balance().add(amount);
            log.debug("Current balance: {}, New balance: {}", account.balance(), newBalance);

            // 4. Update account balance
            Account updatedAccount = account.withBalance(newBalance).withIncrementedVersion();
            accountRepository.save(updatedAccount);

            // 5. Create and save transaction
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                accountId,
                amount,
                TransactionType.CREDIT,
                account.currency(),
                newBalance,
                LocalDateTime.now(),
                TransactionStatus.COMPLETED
            );

            Transaction savedTransaction = transactionRepository.save(transaction);
            log.info("Credit transaction completed: {}", savedTransaction.transactionId());

            return savedTransaction;
        });
    }

    @Override
    @Transactional
    public Transaction processDebit(UUID accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }

        log.info("Processing debit transaction for account: {}, amount: {}", accountId, amount);

        return distributedLockService.executeWithLock(accountId.toString(), () -> {
            // 1. Find account
            Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId.toString()));

            // 2. Validate account is active
            if (!account.isActive()) {
                throw new InactiveAccountException(account.accountNumber());
            }

            // 3. Validate sufficient balance
            if (!account.hasSufficientBalance(amount)) {
                throw new InsufficientBalanceException(account.accountNumber(), account.balance(), amount);
            }

            // 4. Calculate new balance
            BigDecimal newBalance = account.balance().subtract(amount);
            log.debug("Current balance: {}, New balance: {}", account.balance(), newBalance);

            // 5. Update account balance
            Account updatedAccount = account.withBalance(newBalance).withIncrementedVersion();
            accountRepository.save(updatedAccount);

            // 6. Create and save transaction
            Transaction transaction = new Transaction(
                UUID.randomUUID(),
                accountId,
                amount,
                TransactionType.DEBIT,
                account.currency(),
                newBalance,
                LocalDateTime.now(),
                TransactionStatus.COMPLETED
            );

            Transaction savedTransaction = transactionRepository.save(transaction);
            log.info("Debit transaction completed: {}", savedTransaction.transactionId());

            return savedTransaction;
        });
    }
}

