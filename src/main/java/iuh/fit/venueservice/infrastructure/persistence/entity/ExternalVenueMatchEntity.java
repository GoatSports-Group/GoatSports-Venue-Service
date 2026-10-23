package iuh.fit.venueservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Một kèo ghép trận đã hoàn tất ở sân ngoài (ai-service báo); khoá theo phiên nên báo lại không đếm trùng. */
@Entity
@Table(name = "external_venue_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalVenueMatchEntity {
    @Id
    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "external_venue_id", nullable = false)
    private UUID externalVenueId;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;
}
