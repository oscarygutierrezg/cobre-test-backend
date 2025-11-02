package co.cobre.cbmm.accounts.adapters.in.rest;

import co.cobre.cbmm.accounts.application.dto.PageResponseDTO;
import co.cobre.cbmm.accounts.application.dto.TransactionResponseDTO;
import co.cobre.cbmm.accounts.domain.model.Transaction;
import co.cobre.cbmm.accounts.ports.in.GetTransactionsPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for transaction query operations
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Transaction query endpoints")
public class TransactionController {

    private final GetTransactionsPort getTransactionsPort;

    @GetMapping("/{accountId}/transactions")
    @Operation(
        summary = "Get account transactions",
        description = "Retrieves all transactions for an account with pagination, ordered by creation date (newest first)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transactions retrieved successfully",
            content = @Content(schema = @Schema(implementation = PageResponseDTO.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<PageResponseDTO<TransactionResponseDTO>> getTransactions(
        @Parameter(description = "Account ID", required = true)
        @PathVariable UUID accountId,

        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size", example = "20")
        @RequestParam(defaultValue = "20") int size,

        @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC")
        @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection
    ) {
        log.info("Retrieving transactions for account: {}, page: {}, size: {}, sort: {}",
            accountId, page, size, sortDirection);

        // Validate pagination parameters
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        // Create pageable with sorting by createdAt
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "createdAt"));

        // Get transactions
        Page<Transaction> transactionPage = getTransactionsPort.getTransactionsByAccountId(accountId, pageable);

        // Map to response DTOs
        Page<TransactionResponseDTO> responsePage = transactionPage.map(TransactionResponseDTO::from);

        // Create paginated response
        PageResponseDTO<TransactionResponseDTO> response = PageResponseDTO.from(responsePage);

        log.info("Retrieved {} transactions for account: {} (total: {})",
            response.content().size(), accountId, response.totalElements());

        return ResponseEntity.ok(response);
    }
}

