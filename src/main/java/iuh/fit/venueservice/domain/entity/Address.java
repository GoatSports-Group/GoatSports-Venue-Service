package iuh.fit.venueservice.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Address extends BaseEntity {
    private UUID addressId;
    private String address;
    private String ward;
    private String district;
    private String city;
    private Double latitude;
    private Double longitude;
}
