package co.cobre.cbmm.accounts.application.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic DTO for paginated responses
 */
public record PageResponseDTO<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    boolean empty
) {
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return new PageResponseDTO<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast(),
            page.isEmpty()
        );
    }
}

