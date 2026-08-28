package iuh.fit.venueservice.domain.entity;

import iuh.fit.venueservice.domain.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OwnerApplicationDocument extends BaseEntity {
    private UUID documentId;
    private UUID ownerApplicationId;
    private DocumentType documentType;
    private String fileUrl;
}
