package iuh.fit.venueservice.enums;

import lombok.Getter;

@Getter
public enum SportType {
    FOOTBALL("Bóng đá"),
    BADMINTON("Cầu lông"),
    TENNIS("Tennis"),
    PICKLEBALL("Pickleball"),
    BASKETBALL("Bóng rổ"),
    VOLLEYBALL("Bóng chuyền");

    private final String value;
    SportType(String value) {
        this.value = value;
    }
}
