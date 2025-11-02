package co.cobre.cbmm.accounts.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for Account information
 */
@Schema(description = "Account information")
public record AccountDTO(

    @Schema(description = "Account unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID accountId,

    @Schema(description = "Account number", example = "ACC-2024-001")
    String accountNumber,

    @Schema(description = "Currency code", example = "USD")
    String currency,

    @Schema(description = "Account balance", example = "1500.50")
    BigDecimal balance,

    @Schema(description = "Account status", example = "ACTIVE")
    String status,

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,

    @Schema(description = "Last update timestamp", example = "2024-01-20T14:45:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,

    @Schema(description = "Version for optimistic locking", example = "1")
    Integer version
) {
}

