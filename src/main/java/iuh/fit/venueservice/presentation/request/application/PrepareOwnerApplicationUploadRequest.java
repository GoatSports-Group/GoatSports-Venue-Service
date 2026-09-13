package iuh.fit.venueservice.presentation.request.application;

import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepareOwnerApplicationUploadRequest {
    @Valid
    @NotEmpty
    @Size(min = 4, max = 4)
    private List<DocumentRequest> documents;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentRequest {
        @NotNull
        private OwnerApplicationDocumentSlot slot;
        @NotBlank
        private String fileName;
        @NotBlank
        private String contentType;
        @NotNull
        @Positive
        @Max(2 * 1024 * 1024)
        private Long fileSize;
    }
}
