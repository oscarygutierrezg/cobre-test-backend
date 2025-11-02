package co.cobre.cbmm.accounts.application.service;

import co.cobre.cbmm.accounts.adapters.out.metrics.ErrorMetricsService;
import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import co.cobre.cbmm.accounts.domain.exception.AccountNotFoundException;
import co.cobre.cbmm.accounts.domain.exception.InactiveAccountException;
import co.cobre.cbmm.accounts.domain.exception.InsufficientBalanceException;
import co.cobre.cbmm.accounts.domain.exception.InvalidCurrencyException;
import co.cobre.cbmm.accounts.domain.model.Account;
import co.cobre.cbmm.accounts.domain.model.Transaction;
import co.cobre.cbmm.accounts.domain.model.TransactionStatus;
import co.cobre.cbmm.accounts.domain.model.TransactionType;
import co.cobre.cbmm.accounts.domain.service.DistributedLockService;
import co.cobre.cbmm.accounts.ports.out.AccountRepositoryPort;
import co.cobre.cbmm.accounts.ports.out.TransactionRepositoryPort;
import co.cobre.cbmm.accounts.domain.model.Currency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for asynchronous account processing using Virtual Threads
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncAccountProcessingService {

    private final AccountRepositoryPort accountRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final DistributedLockService distributedLockService;
    private final ErrorMetricsService errorMetricsService;

    /**
     * Process origin account asynchronously using Virtual Threads
     * Retries on optimistic locking failures with exponential backoff
     * Retry parameters are configured in application.yml under retry.optimistic-locking
     */
    @Async("virtualThreadExecutor")
    @Transactional
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttemptsExpression = "#{${retry.optimistic-locking.max-attempts}}",
        backoff = @Backoff(
            delayExpression = "#{${retry.optimistic-locking.initial-delay}}",
            multiplierExpression = "#{${retry.optimistic-locking.multiplier}}",
            maxDelayExpression = "#{${retry.optimistic-locking.max-delay}}"
        )
    )
    public CompletableFuture<UUID> processOriginAccountAsync(CBMMEventDTO event) {
        log.debug("Processing origin account in Virtual Thread: {}", Thread.currentThread());

        try {
            UUID accountId = processOriginAccount(event);
            return CompletableFuture.completedFuture(accountId);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for origin account {}, will retry: {}",
                event.origin().accountId(), e.getMessage());

            // Record metric
            errorMetricsService.recordOptimisticLockingFailure("Account", "processOriginAccount");

            throw e; // Re-throw to trigger @Retryable
        } catch (Exception e) {
            log.error("Error in async origin account processing: {}", e.getMessage());

            // Record metric
            errorMetricsService.recordError("async_processing_error", "processOriginAccount", e);

            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Process destination account asynchronously using Virtual Threads
     * Retries on optimistic locking failures with exponential backoff
     * Retry parameters are configured in application.yml under retry.optimistic-locking
     */
    @Async("virtualThreadExecutor")
    @Transactional
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttemptsExpression = "#{${retry.optimistic-locking.max-attempts}}",
        backoff = @Backoff(
            delayExpression = "#{${retry.optimistic-locking.initial-delay}}",
            multiplierExpression = "#{${retry.optimistic-locking.multiplier}}",
            maxDelayExpression = "#{${retry.optimistic-locking.max-delay}}"
        )
    )
    public CompletableFuture<UUID> processDestinationAccountAsync(CBMMEventDTO event) {
        log.debug("Processing destination account in Virtual Thread: {}", Thread.currentThread());

        try {
            UUID accountId = processDestinationAccount(event);
            return CompletableFuture.completedFuture(accountId);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Optimistic locking failure for destination account {}, will retry: {}",
                event.destination().accountId(), e.getMessage());

            // Record metric
            errorMetricsService.recordOptimisticLockingFailure("Account", "processDestinationAccount");

            throw e; // Re-throw to trigger @Retryable
        } catch (Exception e) {
            log.error("Error in async destination account processing: {}", e.getMessage());

            // Record metric
            errorMetricsService.recordError("async_processing_error", "processDestinationAccount", e);

            return CompletableFuture.failedFuture(e);
        }
    }

    private UUID processOriginAccount(CBMMEventDTO event) {
        String accountNumber = event.origin().accountId();
        BigDecimal amount = event.origin().amount();
        String currencyCode = event.origin().currency();

        log.info("Processing origin account: {} - Debit: {} {}", accountNumber, amount, currencyCode);

        return distributedLockService.executeWithLock(accountNumber, () -> {
            try {
                Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> {
                        errorMetricsService.recordError("account_not_found", "processOriginAccount",
                            new AccountNotFoundException(accountNumber));
                        return new AccountNotFoundException(accountNumber);
                    });

                if (!account.isActive()) {
                    errorMetricsService.recordError("inactive_account", "processOriginAccount",
                        new InactiveAccountException(accountNumber));
                    throw new InactiveAccountException(accountNumber);
                }

                if (!account.currency().getCode().equals(currencyCode)) {
                    errorMetricsService.recordValidationError("currency_mismatch", "origin.currency");
                    throw new InvalidCurrencyException(
                        String.format("Origin account currency mismatch. Expected: %s, Got: %s",
                            account.currency().getCode(), currencyCode)
                    );
                }

                if (!account.hasSufficientBalance(amount)) {
                    errorMetricsService.recordInsufficientBalanceError(accountNumber, currencyCode);
                    throw new InsufficientBalanceException(accountNumber, account.balance(), amount);
                }

                BigDecimal newBalance = account.balance().subtract(amount);
                log.debug("Origin account {} - Current balance: {}, New balance: {}",
                    accountNumber, account.balance(), newBalance);

                // Create updated account - version will be handled by JPA @Version
                Account updatedAccount = new Account(
                    account.accountId(),
                    account.accountNumber(),
                    account.currency(),
                    newBalance,
                    account.status(),
                    account.createdAt(),
                    LocalDateTime.now(),
                    account.version() // Keep current version, JPA will increment it
                );
                accountRepository.save(updatedAccount);

                Transaction transaction = new Transaction(
                    null,
                    account.accountId(),
                    amount,
                    TransactionType.DEBIT,
                    Currency.fromCode(currencyCode),
                    newBalance,
                    LocalDateTime.now(),
                    TransactionStatus.COMPLETED
                );

                transactionRepository.save(transaction);
                log.info("Origin debit transaction completed for account: {}", accountNumber);

                return account.accountId();
            } catch (ObjectOptimisticLockingFailureException e) {
                errorMetricsService.recordOptimisticLockingFailure("Account", "processOriginAccount");
                throw e;
            } catch (RuntimeException e) {
                // Any other runtime exception
                if (!(e instanceof AccountNotFoundException ||
                      e instanceof InactiveAccountException ||
                      e instanceof InvalidCurrencyException ||
                      e instanceof InsufficientBalanceException)) {
                    errorMetricsService.recordError("origin_account_processing_error", "processOriginAccount", e);
                }
                throw e;
            }
        });
    }

    private UUID processDestinationAccount(CBMMEventDTO event) {
        String accountNumber = event.destination().accountId();
        BigDecimal amount = event.destination().amount();
        String currencyCode = event.destination().currency();

        log.info("Processing destination account: {} - Credit: {} {}", accountNumber, amount, currencyCode);

        return distributedLockService.executeWithLock(accountNumber, () -> {
            try {
                Account account = accountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> {
                        errorMetricsService.recordError("account_not_found", "processDestinationAccount",
                            new AccountNotFoundException(accountNumber));
                        return new AccountNotFoundException(accountNumber);
                    });

                if (!account.isActive()) {
                    errorMetricsService.recordError("inactive_account", "processDestinationAccount",
                        new InactiveAccountException(accountNumber));
                    throw new InactiveAccountException(accountNumber);
                }

                if (!account.currency().getCode().equals(currencyCode)) {
                    errorMetricsService.recordValidationError("currency_mismatch", "destination.currency");
                    throw new InvalidCurrencyException(
                        String.format("Destination account currency mismatch. Expected: %s, Got: %s",
                            account.currency().getCode(), currencyCode)
                    );
                }

                BigDecimal newBalance = account.balance().add(amount);
                log.debug("Destination account {} - Current balance: {}, New balance: {}",
                    accountNumber, account.balance(), newBalance);

                // Create updated account - version will be handled by JPA @Version
                Account updatedAccount = new Account(
                    account.accountId(),
                    account.accountNumber(),
                    account.currency(),
                    newBalance,
                    account.status(),
                    account.createdAt(),
                    LocalDateTime.now(),
                    account.version() // Keep current version, JPA will increment it
                );
                accountRepository.save(updatedAccount);

                Transaction transaction = new Transaction(
                    null,
                    account.accountId(),
                    amount,
                    TransactionType.CREDIT,
                    Currency.fromCode(currencyCode),
                    newBalance,
                    LocalDateTime.now(),
                    TransactionStatus.COMPLETED
                );

                transactionRepository.save(transaction);
                log.info("Destination credit transaction completed for account: {}", accountNumber);

                return account.accountId();
            } catch (ObjectOptimisticLockingFailureException e) {
                errorMetricsService.recordOptimisticLockingFailure("Account", "processDestinationAccount");
                throw e;
            } catch (RuntimeException e) {
                // Any other runtime exception
                if (!(e instanceof AccountNotFoundException ||
                      e instanceof InactiveAccountException ||
                      e instanceof InvalidCurrencyException)) {
                    errorMetricsService.recordError("destination_account_processing_error", "processDestinationAccount", e);
                }
                throw e;
            }
        });
    }
}

