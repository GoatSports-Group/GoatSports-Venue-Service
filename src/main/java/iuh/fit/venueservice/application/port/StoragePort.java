package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.dto.request.file.MoveFileRequest;
import iuh.fit.venueservice.application.dto.request.file.PresignedUrlRequestCommand;
import iuh.fit.venueservice.application.dto.response.file.PresignedUrlResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.List;
import java.util.UUID;

public interface StoragePort {
    List<PresignedUrlResponseCommand> generatePresignedUrls(
            List<PresignedUrlRequestCommand> requests,
            UUID userId
    ) throws ServiceException;

    List<String> confirmFile(List<FileMoveRequest> requests) throws ServiceException;

    void moveFile(MoveFileRequest request) throws ServiceException;

    void delete(String key) throws ServiceException;
}
