package iuh.fit.venueservice.domain.enums;

import lombok.Getter;

@Getter
public enum DocumentType {
    ID_CARD("CCCD"),
    BUSINESS_LICENSE("Giấy phép hoạt động"),
    VENUE_IMAGE("Ảnh sân"),
    OTHER("Khác");

    private final String value;
    DocumentType(String value) {
        this.value = value;
    }
}
