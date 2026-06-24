package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.file.UploadRequestCommand;
import iuh.fit.venueservice.application.dto.response.file.DownloadResponseCommand;
import iuh.fit.venueservice.application.dto.response.file.FileResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.StorageService;
import iuh.fit.venueservice.application.service.impl.StorageServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class StorageDecorator implements StorageService {
    private final StorageServiceImpl storageServiceImpl;

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public CompletableFuture<FileResponseCommand> upload(UploadRequestCommand command) throws ServiceException {
        return storageServiceImpl.upload(command);
    }

    @Override
    public String getFileUrl(String key) {
        return storageServiceImpl.getFileUrl(key);
    }

    @Override
    public DownloadResponseCommand download(String key) {
        return storageServiceImpl.download(key);
    }

    @Override
    public void delete(String key) {
        storageServiceImpl.delete(key);
    }
}
