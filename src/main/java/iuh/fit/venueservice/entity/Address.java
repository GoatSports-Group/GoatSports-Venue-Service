package iuh.fit.venueservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID addressId;
    private String address;
    private String ward;
    private String district;
    private String city;
    private String province;

    @OneToOne
    @JoinColumn(name = "owner_application_id", nullable = false, unique = true)
    private OwnerApplication ownerApplication;
}
