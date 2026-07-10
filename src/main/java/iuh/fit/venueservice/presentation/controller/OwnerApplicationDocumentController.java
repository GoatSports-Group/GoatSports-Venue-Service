package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.OwnerApplicationDocumentService;
import iuh.fit.venueservice.presentation.request.document.CreateDocumentsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class OwnerApplicationDocumentController {
    private final OwnerApplicationDocumentService ownerApplicationDocumentService;

    @PostMapping
    public ResponseEntity<Void> createDocuments(
            @Valid @RequestBody CreateDocumentsRequest request
    ) throws ServiceException
    {
        List<FileMoveRequest> requests = request.getDocumentKeys().stream()
                .map(FileMoveRequest::new)
                .toList();

        ownerApplicationDocumentService.createOwnerApplicationDocument(request.getOwnerApplicationId(), requests);

        return ResponseEntity.ok().build();
    }

}
