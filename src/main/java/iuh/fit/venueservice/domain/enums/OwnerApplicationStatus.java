package iuh.fit.venueservice.domain.enums;

import lombok.Getter;

@Getter
public enum OwnerApplicationStatus {
    PENDING("Chờ xác nhận"),
    APPROVED("Chấp nhận"),
    REJECTED("Từ chối"),
    CANCELLED("Hủy");

    private final String value;
    OwnerApplicationStatus(String value) {
        this.value = value;
    }
}
