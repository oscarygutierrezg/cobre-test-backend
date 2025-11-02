package co.cobre.cbmm.accounts.adapters.out.persistence;

import co.cobre.cbmm.accounts.adapters.out.persistence.entity.CBMMEventEntity;
import co.cobre.cbmm.accounts.adapters.out.persistence.mapper.CBMMEventMapper;
import co.cobre.cbmm.accounts.adapters.out.persistence.repository.CBMMEventJpaRepository;
import co.cobre.cbmm.accounts.application.dto.CBMMEventDTO;
import co.cobre.cbmm.accounts.ports.out.CBMMEventRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository Adapter for CBMM Event persistence (Driven Adapter)
 * Implements the CBMMEventRepositoryPort using JPA
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CBMMEventRepositoryAdapter implements CBMMEventRepositoryPort {

    private final CBMMEventJpaRepository jpaRepository;
    private final CBMMEventMapper mapper;

    @Override
    @Transactional
    public CBMMEventDTO save(CBMMEventDTO event) {
        log.debug("Saving CBMM event: {}", event.eventId());

        CBMMEventEntity entity = mapper.toEntity(event, "PENDING");
        CBMMEventEntity saved = jpaRepository.save(entity);

        log.info("CBMM event saved successfully: {}", saved.getEventId());
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional
    public void updateStatus(String eventId, String status, Integer retryCount) {
        log.debug("Updating CBMM event status: eventId={}, status={}, retryCount={}",
            eventId, status, retryCount);

        jpaRepository.findByEventId(eventId).ifPresent(entity -> {
            mapper.updateStatus(entity, status, retryCount);
            jpaRepository.save(entity);
            log.info("CBMM event status updated: eventId={}, newStatus={}", eventId, status);
        });
    }
}

