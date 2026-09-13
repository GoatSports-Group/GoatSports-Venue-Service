package iuh.fit.venueservice.application.dto.request.file;

public record PresignedUrlRequestCommand(
        String fileName,
        String contentType,
        String folder,
        Long contentLength
) {}
