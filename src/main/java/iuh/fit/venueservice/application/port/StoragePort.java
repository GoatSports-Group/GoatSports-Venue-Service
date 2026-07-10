package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.exception.ServiceException;

import java.util.List;

public interface StoragePort {
    List<String> confirmFile(List<FileMoveRequest> requests) throws ServiceException;
}
