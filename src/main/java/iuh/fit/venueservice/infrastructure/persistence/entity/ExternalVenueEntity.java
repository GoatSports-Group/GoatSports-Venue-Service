package iuh.fit.venueservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_venues")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalVenueEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "external_venue_id")
    private UUID externalVenueId;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "source_id", nullable = false, length = 60)
    private String sourceId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "sport_types", nullable = false, length = 200)
    private String sportTypes;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 60)
    private String phone;

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "opening_hours", length = 200)
    private String openingHours;

    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;
}
