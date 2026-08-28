package iuh.fit.venueservice.domain.enums;

import lombok.Getter;

@Getter
public enum DocumentType {
    IDENTITY_CARD("CCCD"),
    BUSINESS_LICENSE("Giấy phép hoạt động"),
    VENUE_PHOTO("Ảnh sân");

    private final String value;
    DocumentType(String value) {
        this.value = value;
    }
}
