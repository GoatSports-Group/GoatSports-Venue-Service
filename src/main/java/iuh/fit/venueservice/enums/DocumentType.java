package iuh.fit.venueservice.enums;

import lombok.Getter;

@Getter
public enum DocumentType {
    ID_CARD_FRONT("CCCD mặt trước"),
    ID_CARD_BACK("CCCD mặt sau"),
    BUSINESS_LICENSE("Giấy phép hoạt động"),
    VENUE_IMAGE("Ảnh sân");

    private final String value;
    DocumentType(String value) {
        this.value = value;
    }
}
