package co.cobre.cbmm.accounts.domain.exception;

import lombok.Getter;

/**
 * Exception thrown when a file size exceeds the maximum allowed
 */
@Getter
public class FileSizeExceededException extends RuntimeException {

    private final long fileSize;
    private final long maxSize;

    public FileSizeExceededException(long fileSize, long maxSize) {
        super(String.format("File size (%.2f MB) exceeds maximum allowed size of %.2f MB",
            fileSize / 1024.0 / 1024.0, maxSize / 1024.0 / 1024.0));
        this.fileSize = fileSize;
        this.maxSize = maxSize;
    }

}

