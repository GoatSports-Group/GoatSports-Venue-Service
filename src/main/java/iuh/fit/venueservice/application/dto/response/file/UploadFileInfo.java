package iuh.fit.venueservice.application.dto.response.file;

public record UploadFileInfo(
        byte[] content,
        String originalFilename,
        String contentType
) {}
