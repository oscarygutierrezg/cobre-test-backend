package co.cobre.cbmm.accounts.adapters.out.persistence.mapper;

import co.cobre.cbmm.accounts.adapters.out.persistence.entity.CBMMEventEntity;
import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper for CBMM Event Entity <-> DTO
 */
@Component
public class CBMMEventMapper {

    private static final String FIELD_ACCOUNT_ID = "account_id";
    private static final String FIELD_CURRENCY = "currency";
    private static final String FIELD_AMOUNT = "amount";

    /**
     * Convert Entity to DTO
     */
    public CBMMEventDTO toDTO(CBMMEventEntity entity) {
        if (entity == null) {
            return null;
        }

        // Extract origin data from Map
        Map<String, Object> originData = entity.getOriginData();
        CBMMEventDTO.AccountOperationDTO origin = new CBMMEventDTO.AccountOperationDTO(
            (String) originData.get(FIELD_ACCOUNT_ID),
            (String) originData.get(FIELD_CURRENCY),
            new BigDecimal(originData.get(FIELD_AMOUNT).toString())
        );

        // Extract destination data from Map
        Map<String, Object> destData = entity.getDestinationData();
        CBMMEventDTO.AccountOperationDTO destination = new CBMMEventDTO.AccountOperationDTO(
            (String) destData.get(FIELD_ACCOUNT_ID),
            (String) destData.get(FIELD_CURRENCY),
            new BigDecimal(destData.get(FIELD_AMOUNT).toString())
        );

        return new CBMMEventDTO(
            entity.getEventId(),
            entity.getEventType(),
            entity.getOperationDate(),
            origin,
            destination
        );
    }

    /**
     * Convert DTO to Entity
     */
    public CBMMEventEntity toEntity(CBMMEventDTO dto, String status) {
        if (dto == null) {
            return null;
        }

        // Convert origin to Map
        Map<String, Object> originData = new HashMap<>();
        originData.put(FIELD_ACCOUNT_ID, dto.origin().accountId());
        originData.put(FIELD_CURRENCY, dto.origin().currency());
        originData.put(FIELD_AMOUNT, dto.origin().amount());

        // Convert destination to Map
        Map<String, Object> destData = new HashMap<>();
        destData.put(FIELD_ACCOUNT_ID, dto.destination().accountId());
        destData.put(FIELD_CURRENCY, dto.destination().currency());
        destData.put(FIELD_AMOUNT, dto.destination().amount());

        return CBMMEventEntity.builder()
            .eventId(dto.eventId())
            .eventType(dto.eventType())
            .operationDate(dto.operationDate())
            .originData(originData)
            .destinationData(destData)
            .status(CBMMEventEntity.EventStatus.valueOf(status))
            .retryCount(0)
            .build();
    }

    /**
     * Update entity status
     */
    public void updateStatus(CBMMEventEntity entity, String status, Integer retryCount) {
        if (entity != null) {
            entity.setStatus(CBMMEventEntity.EventStatus.valueOf(status));
            entity.setRetryCount(retryCount);
        }
    }
}

