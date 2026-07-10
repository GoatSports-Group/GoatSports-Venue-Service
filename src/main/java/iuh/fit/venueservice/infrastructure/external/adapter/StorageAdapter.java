package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
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

        if(response.getError() != null || response.getMessage() != null) {
            throw new ServiceException("Xác nhận tài liệu thất bại: " + response.getMessage());
        }

        return response.getData();
    }
}
