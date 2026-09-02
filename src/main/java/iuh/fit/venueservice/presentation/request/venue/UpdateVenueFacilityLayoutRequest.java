package iuh.fit.venueservice.presentation.request.venue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateVenueFacilityLayoutRequest(
        @NotNull @Size(max = 500) List<@Valid Item> items,
        @NotEmpty @Size(max = 100) List<@Valid Zone> zones
) {
    private static final String ITEM_TYPES =
            "COURT|RECEPTION|ENTRANCE|PARKING|LOCKER|WC|WAITING|CAFE|STORAGE|CUSTOM";

    public record Item(
            @NotBlank @Size(max = 120) String id,
            @NotBlank @Pattern(regexp = ITEM_TYPES) String type,
            UUID courtId,
            @NotBlank @Size(max = 80) String label,
            @DecimalMin("0") @DecimalMax("100000") double x,
            @DecimalMin("0") @DecimalMax("100000") double y,
            @DecimalMin("40") @DecimalMax("100000") double width,
            @DecimalMin("40") @DecimalMax("100000") double height,
            @DecimalMin("0") @DecimalMax("359") double rotation,
            @Size(max = 120) String zoneId,
            @Size(max = 80) String icon
    ) { }

    public record Zone(
            @NotBlank @Size(max = 120) String id,
            @NotBlank @Size(max = 80) String name,
            @DecimalMin("0") @DecimalMax("100000") double x,
            @DecimalMin("0") @DecimalMax("100000") double y,
            @DecimalMin("180") @DecimalMax("100000") double width,
            @DecimalMin("120") @DecimalMax("100000") double height
    ) { }
}
