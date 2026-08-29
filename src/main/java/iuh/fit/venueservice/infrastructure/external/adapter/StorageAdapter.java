package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.dto.request.file.MoveFileRequest;
import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.infrastructure.external.client.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StorageAdapter implements StoragePort {
    private final StorageClient storageClient;

    @Override
    public List<String> confirmFile(List<FileMoveRequest> requests) throws ServiceException {
        BaseResponse<List<String>> response = storageClient.confirmFile(requests);

        if (response == null || response.getError() != null) {
            String message = response == null ? "Storage Service không phản hồi" : String.valueOf(response.getMessage());
            throw new ServiceException("Xác nhận tài liệu thất bại: " + message);
        }

        return response.getData();
    }

    @Override
    public void moveFile(MoveFileRequest request) throws ServiceException {
        storageClient.moveFile(request);
    }
}
