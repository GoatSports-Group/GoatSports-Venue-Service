package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(
        name = "GOATSPORTS-STORAGE-SERVICE",
        url = "${storage.service.url}",
        path = "/internal/files"
)
public interface StorageClient {
    @PostMapping("/confirm")
    BaseResponse<List<String>> confirmFile(@RequestBody List<FileMoveRequest> requests);
}
