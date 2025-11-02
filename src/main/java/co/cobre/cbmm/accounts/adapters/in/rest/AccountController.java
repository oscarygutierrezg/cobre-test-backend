package co.cobre.cbmm.accounts.adapters.in.rest;

import co.cobre.cbmm.accounts.application.dto.AccountDTO;
import co.cobre.cbmm.accounts.ports.in.GetAccountPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for account operations (Driving Adapter)
 * Uses the driving ports to interact with the application layer
 */
@RestController
@RequestMapping("/api/v1/accounts")
@Validated
@Tag(name = "Accounts", description = "Account management operations")
@RequiredArgsConstructor
public class AccountController {

    private final GetAccountPort getAccountPort;

    @Operation(
        summary = "Get account by account number",
        description = "Returns the account information for the specified account number"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Account retrieved successfully",
            content = @Content(schema = @Schema(implementation = AccountDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account not found"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid account number provided"
        )
    })
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDTO> getAccountByNumber(
        @Parameter(description = "Account number", example = "ACC-2024-001", required = true)
        @PathVariable
        @NotBlank(message = "Account number cannot be blank")
        String accountNumber
    ) {
        AccountDTO account = getAccountPort.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(account);
    }
}

