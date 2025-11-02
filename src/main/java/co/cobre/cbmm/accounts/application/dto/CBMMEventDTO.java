package co.cobre.cbmm.accounts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * DTO for CBMM (Cross Border Money Movement) events from Kafka
 *
 * Example JSON:
 * {
 *   "event_id": "cbmm_20250909_000123",
 *   "event_type": "cross_border_money_movement",
 *   "operation_date": "2025-09-09T15:32:10Z",
 *   "origin": {
 *     "account_id": "ACC123456789",
 *     "currency": "COP",
 *     "amount": 15000.50
 *   },
 *   "destination": {
 *     "account_id": "ACC987654321",
 *     "currency": "USD",
 *     "amount": 880.25
 *   }
 * }
 */
public record CBMMEventDTO(
    @JsonProperty("event_id")
    String eventId,

    @JsonProperty("event_type")
    String eventType,

    @JsonProperty("operation_date")
    ZonedDateTime operationDate,

    @JsonProperty("origin")
    AccountOperationDTO origin,

    @JsonProperty("destination")
    AccountOperationDTO destination
) {
    /**
     * Nested DTO for account operation details
     */
    public record AccountOperationDTO(
        @JsonProperty("account_id")
        String accountId,

        @JsonProperty("currency")
        String currency,

        @JsonProperty("amount")
        BigDecimal amount
    ) {
    }

}

