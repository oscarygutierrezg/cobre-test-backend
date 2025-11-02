package co.cobre.cbmm.accounts.unit.application.usecase;

import co.cobre.cbmm.accounts.application.usecase.GetTransactionsUseCase;
import co.cobre.cbmm.accounts.domain.model.Currency;
import co.cobre.cbmm.accounts.domain.model.Transaction;
import co.cobre.cbmm.accounts.domain.model.TransactionStatus;
import co.cobre.cbmm.accounts.domain.model.TransactionType;
import co.cobre.cbmm.accounts.ports.out.TransactionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetTransactionsUseCaseUnitTest {

    @Mock
    private TransactionRepositoryPort transactionRepository;

    @InjectMocks
    private GetTransactionsUseCase getTransactionsUseCase;

    private UUID accountId;
    private Transaction transaction1;
    private Transaction transaction2;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();

        transaction1 = new Transaction(
            UUID.randomUUID(),
            accountId,
            new BigDecimal("100.00"),
            TransactionType.CREDIT,
            Currency.USD,
            new BigDecimal("1100.00"),
            LocalDateTime.now(),
            TransactionStatus.COMPLETED
        );

        transaction2 = new Transaction(
            UUID.randomUUID(),
            accountId,
            new BigDecimal("50.00"),
            TransactionType.DEBIT,
            Currency.USD,
            new BigDecimal("1050.00"),
            LocalDateTime.now(),
            TransactionStatus.COMPLETED
        );
    }

    @Nested
    @DisplayName("Get transactions by account ID tests")
    class GetTransactionsByAccountIdTests {

        @Test
        @DisplayName("Given account with transactions, when getTransactionsByAccountId, then return paginated transactions")
        void givenAccountWithTransactions_whenGetTransactionsByAccountId_thenReturnPaginatedTransactions() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
            List<Transaction> transactions = Arrays.asList(transaction1, transaction2);
            Page<Transaction> expectedPage = new PageImpl<>(transactions, pageable, 2);

            when(transactionRepository.findByAccountIdPaginated(accountId, pageable))
                .thenReturn(expectedPage);

            // Act
            Page<Transaction> result = getTransactionsUseCase.getTransactionsByAccountId(accountId, pageable);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            assertEquals(2, result.getTotalElements());
            assertTrue(result.getContent().contains(transaction1));
            assertTrue(result.getContent().contains(transaction2));
            verify(transactionRepository).findByAccountIdPaginated(accountId, pageable);
        }

        @Test
        @DisplayName("Given account with no transactions, when getTransactionsByAccountId, then return empty page")
        void givenAccountWithNoTransactions_whenGetTransactionsByAccountId_thenReturnEmptyPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 20);
            Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(transactionRepository.findByAccountIdPaginated(accountId, pageable))
                .thenReturn(emptyPage);

            // Act
            Page<Transaction> result = getTransactionsUseCase.getTransactionsByAccountId(accountId, pageable);

            // Assert
            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertEquals(0, result.getTotalElements());
            verify(transactionRepository).findByAccountIdPaginated(accountId, pageable);
        }

        @Test
        @DisplayName("Given custom pageable, when getTransactionsByAccountId, then return correct page")
        void givenCustomPageable_whenGetTransactionsByAccountId_thenReturnCorrectPage() {
            // Arrange
            Pageable pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "createdAt"));
            List<Transaction> transactions = Collections.singletonList(transaction1);
            Page<Transaction> expectedPage = new PageImpl<>(transactions, pageable, 15);

            when(transactionRepository.findByAccountIdPaginated(accountId, pageable))
                .thenReturn(expectedPage);

            // Act
            Page<Transaction> result = getTransactionsUseCase.getTransactionsByAccountId(accountId, pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(11, result.getTotalElements());
            assertEquals(1, result.getNumber());
            assertEquals(10, result.getSize());
            verify(transactionRepository).findByAccountIdPaginated(accountId, pageable);
        }

        @Test
        @DisplayName("Given multiple pages of transactions, when getTransactionsByAccountId, then return first page")
        void givenMultiplePagesOfTransactions_whenGetTransactionsByAccountId_thenReturnFirstPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 1);
            List<Transaction> transactions = Collections.singletonList(transaction1);
            Page<Transaction> expectedPage = new PageImpl<>(transactions, pageable, 2);

            when(transactionRepository.findByAccountIdPaginated(accountId, pageable))
                .thenReturn(expectedPage);

            // Act
            Page<Transaction> result = getTransactionsUseCase.getTransactionsByAccountId(accountId, pageable);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(2, result.getTotalElements());
            assertEquals(2, result.getTotalPages());
            assertFalse(result.isLast());
            verify(transactionRepository).findByAccountIdPaginated(accountId, pageable);
        }

        @Test
        @DisplayName("Given different account IDs, when getTransactionsByAccountId, then call repository with correct ID")
        void givenDifferentAccountIds_whenGetTransactionsByAccountId_thenCallRepositoryWithCorrectId() {
            // Arrange
            UUID anotherAccountId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 20);
            Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(transactionRepository.findByAccountIdPaginated(anotherAccountId, pageable))
                .thenReturn(emptyPage);

            // Act
            getTransactionsUseCase.getTransactionsByAccountId(anotherAccountId, pageable);

            // Assert
            verify(transactionRepository).findByAccountIdPaginated(eq(anotherAccountId), eq(pageable));
            verify(transactionRepository, never()).findByAccountIdPaginated(eq(accountId), any());
        }
    }
}

