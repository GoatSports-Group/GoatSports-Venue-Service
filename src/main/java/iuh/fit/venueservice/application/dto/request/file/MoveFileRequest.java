package iuh.fit.venueservice.application.dto.request.file;

import java.util.List;

public record MoveFileRequest(
        List<FileMoveRequest> requests,
        Long processInstanceKey
) {}
