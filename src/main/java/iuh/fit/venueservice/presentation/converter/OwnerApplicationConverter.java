package iuh.fit.venueservice.presentation.converter;

import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.presentation.response.venue.OwnerApplicationResponse;

import java.util.List;
import java.util.stream.Collectors;

public class OwnerApplicationConverter {

    public static OwnerApplicationResponse toResponse(OwnerApplication app) {
        if (app == null) return null;

        OwnerApplicationResponse.AddressResponse addr = null;
        if (app.getAddress() != null) {
            addr = OwnerApplicationResponse.AddressResponse.builder()
                    .addressId(app.getAddress().getAddressId())
                    .address(app.getAddress().getAddress())
                    .ward(app.getAddress().getWard())
                    .district(app.getAddress().getDistrict())
                    .city(app.getAddress().getCity())
                    .province(app.getAddress().getProvince())
                    .build();
        }

        List<OwnerApplicationResponse.DocumentResponse> docs = null;
        if (app.getDocuments() != null) {
            docs = app.getDocuments().stream()
                    .map(doc -> OwnerApplicationResponse.DocumentResponse.builder()
                            .ownerApplicationDocumentId(doc.getOwnerApplicationDocumentId())
                            .documentType(doc.getDocumentType())
                            .fileUrl(doc.getFileUrl())
                            .build())
                    .collect(Collectors.toList());
        }

        return OwnerApplicationResponse.builder()
                .ownerApplicationId(app.getOwnerApplicationId())
                .userId(app.getUserId())
                .fullName(app.getFullName())
                .phone(app.getPhone())
                .email(app.getEmail())
                .businessName(app.getBusinessName())
                .businessType(app.getBusinessType())
                .taxCode(app.getTaxCode())
                .identityNumber(app.getIdentityNumber())
                .status(app.getStatus())
                .rejectReason(app.getRejectReason())
                .reviewerId(app.getReviewerId())
                .reviewedAt(app.getReviewedAt())
                .address(addr)
                .documents(docs)
                .build();
    }

}
