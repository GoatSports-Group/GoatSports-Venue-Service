package iuh.fit.venueservice.application.usecase;

import iuh.fit.venueservice.application.dto.request.file.UploadRequestCommand;
import iuh.fit.venueservice.application.dto.response.file.DownloadResponseCommand;
import iuh.fit.venueservice.application.dto.response.file.FileResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.util.FileUtil;

import java.util.concurrent.CompletableFuture;

public class StorageUseCase {

    private final StoragePort storagePort;

    public StorageUseCase(StoragePort storagePort) {
        this.storagePort = storagePort;
    }

    public CompletableFuture<FileResponseCommand> upload(UploadRequestCommand command) throws ServiceException {
        FileUtil.assertAllowed(command.originalFileName(), command.content(), command.contentType());

        return storagePort.upload(
                command.content(),
                command.originalFileName(),
                command.contentType(),
                command.folder()
        );
    }

    public String getFileUrl(String key) {
        return storagePort.getFileUrl(key);
    }

    public DownloadResponseCommand download(String key) {
        return storagePort.download(key);
    }

    public void delete(String key) {
        storagePort.delete(key);
    }
}
