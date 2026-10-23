package iuh.fit.venueservice.infrastructure.persistence.entity;

import iuh.fit.venueservice.domain.enums.ExternalVenueSuggestionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "external_venue_suggestions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalVenueSuggestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "suggestion_id")
    private UUID suggestionId;

    @Column(name = "suggested_by", nullable = false)
    private UUID suggestedBy;

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

    @Column(name = "note", length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExternalVenueSuggestionStatus status;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "external_venue_id")
    private UUID externalVenueId;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
