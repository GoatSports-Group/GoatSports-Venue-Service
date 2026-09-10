package iuh.fit.venueservice.application.dto.response.file;

public record PresignedUrlResponseCommand(
        String uploadUrl,
        String objectKey
) {}
