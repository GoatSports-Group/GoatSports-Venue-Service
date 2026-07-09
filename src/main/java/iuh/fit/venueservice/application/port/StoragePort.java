package iuh.fit.venueservice.application.port;

import iuh.fit.venueservice.application.dto.response.file.FileResponse;
import iuh.fit.venueservice.application.dto.response.file.UploadFileInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StoragePort {
    CompletableFuture<List<FileResponse>> uploadMultiple(List<UploadFileInfo> files, String folder);

    String getFileUrl(String key);
}
