package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.DocumentType;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerApplicationDocument {
    private UUID ownerApplicationDocumentId;
    private DocumentType documentType;
    private String fileUrl;
}
