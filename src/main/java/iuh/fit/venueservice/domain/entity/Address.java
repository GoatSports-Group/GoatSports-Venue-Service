package iuh.fit.venueservice.domain.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    private UUID addressId;
    private String address;
    private String ward;
    private String district;
    private String city;
    private String province;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
