package iuh.fit.venueservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/** Một người chơi muốn đặt online sân ngoài này: mỗi người một lượt mỗi sân. */
@Entity
@Table(name = "external_venue_wants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalVenueWantEntity {
    @EmbeddedId
    private Key id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        @Column(name = "external_venue_id", nullable = false)
        private UUID externalVenueId;

        @Column(name = "user_id", nullable = false)
        private UUID userId;
    }
}
