package iuh.fit.venueservice.domain.enums;

import lombok.Getter;

@Getter
public enum BusinessType {
    INDIVIDUAL("Cá nhân"),
    COMPANY("Công ty");

    private final String value;
    BusinessType(String value) {
        this.value = value;
    }
}
