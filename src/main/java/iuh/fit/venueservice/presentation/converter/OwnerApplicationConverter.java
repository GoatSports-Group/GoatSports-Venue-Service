package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.application.dto.response.application.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.presentation.response.application.OwnerApplicationResponse;

import java.util.Set;
import java.util.stream.Collectors;

public class OwnerApplicationConverter {

    public static OwnerApplicationResponse toResponse(OwnerApplicationResponseCommand app) {
        if (app == null) return null;

        OwnerApplicationResponse.AddressResponse addr = null;
        if (app.address() != null) {
            addr = OwnerApplicationResponse.AddressResponse.builder()
                    .addressId(app.address().getAddressId())
                    .address(app.address().getAddress())
                    .ward(app.address().getWard())
                    .district(app.address().getDistrict())
                    .city(app.address().getCity())
                    .province(app.address().getProvince())
                    .build();
        }

        Set<OwnerApplicationResponse.DocumentResponse> docs = null;
        if (app.documents() != null) {
            docs = app.documents().stream()
                    .map(doc -> OwnerApplicationResponse.DocumentResponse.builder()
                            .ownerApplicationDocumentId(doc.getOwnerApplicationDocumentId())
                            .documentType(doc.getDocumentType())
                            .fileUrl(doc.getFileUrl())
                            .build()
                    )
                    .collect(Collectors.toSet());
        }

        return OwnerApplicationResponse.builder()
                .ownerApplicationId(app.ownerApplicationId())
                .userId(app.userId())
                .fullName(app.fullName())
                .phone(app.phone())
                .email(app.email())
                .businessName(app.businessName())
                .businessType(app.businessType().name())
                .taxCode(app.taxCode())
                .identityNumber(app.identityNumber())
                .status(app.status().name())
                .rejectReason(app.rejectReason())
                .createdAt(app.createdAt())
                .reviewerId(app.reviewerId())
                .reviewedAt(app.reviewedAt())
                .address(addr)
                .documents(docs)
                .build();
    }

}
