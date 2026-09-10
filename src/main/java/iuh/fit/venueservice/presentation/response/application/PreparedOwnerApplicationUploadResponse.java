package iuh.fit.venueservice.presentation.response.application;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreparedOwnerApplicationUploadResponse {
    private UUID ownerApplicationId;
    private List<DocumentResponse> documents;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentResponse {
        private String slot;
        private String uploadUrl;
        private String objectKey;
    }
}
