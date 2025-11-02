package co.cobre.cbmm.accounts.unit.application.dto;

import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CBMMEventDTO Deserialization Tests")
class CBMMEventDTODeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should deserialize valid CBMM event JSON")
    void shouldDeserializeValidCBMMEventJson() throws Exception {
        // Arrange
        String json = """
            {
                "event_id": "cbmm_20250909_000123",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T15:32:10Z",
                "origin": {
                    "account_id": "ACC123456789",
                    "currency": "MXN",
                    "amount": 15000.50
                },
                "destination": {
                    "account_id": "ACC987654321",
                    "currency": "USD",
                    "amount": 880.25
                }
            }
            """;

        // Act
        CBMMEventDTO event = objectMapper.readValue(json, CBMMEventDTO.class);

        // Assert
        assertNotNull(event);
        assertEquals("cbmm_20250909_000123", event.eventId());
        assertEquals("cross_border_money_movement", event.eventType());
        assertNotNull(event.operationDate());

        // Verify origin
        assertNotNull(event.origin());
        assertEquals("ACC123456789", event.origin().accountId());
        assertEquals("MXN", event.origin().currency());
        assertEquals(new BigDecimal("15000.50"), event.origin().amount());

        // Verify destination
        assertNotNull(event.destination());
        assertEquals("ACC987654321", event.destination().accountId());
        assertEquals("USD", event.destination().currency());
        assertEquals(new BigDecimal("880.25"), event.destination().amount());
    }

    @Test
    @DisplayName("Should handle COP currency correctly")
    void shouldHandleCOPCurrencyCorrectly() throws Exception {
        // Arrange
        String json = """
            {
                "event_id": "cbmm_test_001",
                "event_type": "cross_border_money_movement",
                "operation_date": "2025-09-09T10:00:00Z",
                "origin": {
                    "account_id": "ACC111",
                    "currency": "COP",
                    "amount": 1000.00
                },
                "destination": {
                    "account_id": "ACC222",
                    "currency": "EUR",
                    "amount": 50.00
                }
            }
            """;

        // Act
        CBMMEventDTO event = objectMapper.readValue(json, CBMMEventDTO.class);

        // Assert
        assertNotNull(event);
        assertEquals("COP", event.origin().currency());
        assertEquals("EUR", event.destination().currency());
    }
}

