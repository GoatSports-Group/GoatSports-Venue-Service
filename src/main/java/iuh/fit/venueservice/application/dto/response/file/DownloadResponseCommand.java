package iuh.fit.venueservice.application.dto.response.file;

import java.io.InputStream;

public record DownloadResponseCommand(
        InputStream stream,
        String contentType,
        Long contentLength
) {}
