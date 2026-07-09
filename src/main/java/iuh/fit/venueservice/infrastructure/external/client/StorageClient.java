package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.dto.response.file.FileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@FeignClient(
        name = "GOATSPORTS-STORAGE-SERVICE",
        url = "${storage.service.url}",
        path = "/internal/files"
)
public interface StorageClient {
    @PostMapping(value = "/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    BaseResponse<List<FileResponse>> uploadMultipleFiles(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam("folder") String folder
    );

    @GetMapping
    String getFileUrl(@RequestParam("key") String key);
}
