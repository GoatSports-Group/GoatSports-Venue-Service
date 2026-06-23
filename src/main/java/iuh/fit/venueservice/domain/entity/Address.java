package iuh.fit.venueservice.domain.entity;

import lombok.*;

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
}
