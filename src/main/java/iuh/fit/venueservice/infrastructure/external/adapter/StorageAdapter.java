package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.application.dto.response.file.FileResponse;
import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.dto.response.file.UploadFileInfo;
import iuh.fit.venueservice.infrastructure.external.client.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class StorageAdapter implements StoragePort {
    private final StorageClient storageClient;

    @Override
    public CompletableFuture<List<FileResponse>> uploadMultiple(List<UploadFileInfo> files, String folder) {
        return CompletableFuture.supplyAsync(() -> {
            MultipartFile[] multipartFiles = files.stream()
                    .map(f -> new ByteArrayMultipartFile(f.content(), "files", f.originalFilename(), f.contentType()))
                    .toArray(MultipartFile[]::new);

            return storageClient.uploadMultipleFiles(multipartFiles, folder).getData();
        });
    }

    @Override
    public String getFileUrl(String key) {
        return storageClient.getFileUrl(key);
    }
}
