package iuh.fit.venueservice.application.dto.request;

public record UploadRequestCommand(
        byte[] content,
        String originalFileName,
        String contentType,
        String folder
) {}
