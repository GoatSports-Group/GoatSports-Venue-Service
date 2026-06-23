package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.file.UploadRequestCommand;
import iuh.fit.venueservice.application.dto.response.file.DownloadResponseCommand;
import iuh.fit.venueservice.application.dto.response.file.FileResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.usecase.StorageUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class StorageController {
    private final StorageUseCase storageUseCase;

    @PostMapping
    public ResponseEntity<Object> uploadFile(
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "folder") String folder
    ) throws ServiceException, IOException
    {
        if(file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Hãy tải file lên");
        }

        CompletableFuture<FileResponseCommand> response = storageUseCase.upload(
                new UploadRequestCommand(
                        file.getBytes(),
                        file.getOriginalFilename(),
                        file.getContentType(),
                        folder
                )
        );

        return ResponseEntity.status(HttpStatus.OK).body(response.join());
    }

    @PostMapping("/multiple")
    public ResponseEntity<Object> uploadMultipleFiles(
            @RequestParam(name = "files", required = false) MultipartFile[] files,
            @RequestParam(name = "folder") String folder
    ) throws ServiceException, IOException {
        if(files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Hay tải file lên");
        }

        List<CompletableFuture<FileResponseCommand>> futures = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            futures.add(storageUseCase.upload(
                    new UploadRequestCommand(
                            file.getBytes(),
                            file.getOriginalFilename(),
                            file.getContentType(),
                            folder
                    )
            ));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        if (futures.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Tất cả các file tải lên đều trống. Hãy tải file hợp lệ");
        }

        List<FileResponseCommand> responses = futures.stream().map(CompletableFuture::join).toList();

        return ResponseEntity.status(HttpStatus.OK).body(responses);
    }

    @GetMapping
    public ResponseEntity<String> getFileUrl(@RequestParam("key") String key) throws ServiceException {
        if(key == null || key.isEmpty()) {
            throw new ServiceException("Đường dẫn không được để trống");
        }

        String response = storageUseCase.getFileUrl(key);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam("key") String key) throws ServiceException {
        if(key == null || key.isEmpty()) {
            throw new ServiceException("Đường dẫn không được để trống");
        }

        DownloadResponseCommand response = storageUseCase.download(key);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"file.pdf\"")
                .body(new InputStreamResource(response.stream()));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFile(@RequestParam("key") String key) throws ServiceException {
        if(key == null || key.isEmpty()) {
            throw new ServiceException("Đường dẫn không được để trống");
        }
        storageUseCase.delete(key);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
