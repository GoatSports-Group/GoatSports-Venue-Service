package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.dto.request.file.MoveFileRequest;
import iuh.fit.venueservice.application.dto.request.file.PresignedUrlRequestCommand;
import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.dto.response.file.PresignedUrlResponseCommand;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "GOATSPORTS-STORAGE-SERVICE",
        url = "${storage.service.url}",
        path = "/internal/files"
)
public interface StorageClient {
    @PostMapping("/presigned-url")
    BaseResponse<List<PresignedUrlResponseCommand>> generatePresignedUrls(
            @RequestBody List<PresignedUrlRequestCommand> requests,
            @RequestParam UUID userId
    );

    @PostMapping("/confirm")
    BaseResponse<List<String>> confirmFile(@RequestBody List<FileMoveRequest> requests);

    @PostMapping("/move")
    void moveFile(@RequestBody MoveFileRequest request);

    @DeleteMapping
    void delete(@RequestParam String key);

    @GetMapping
    String getFileUrl(@RequestParam String key);
}
