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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "court_holds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourtHoldEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hold_id")
    private UUID holdId;

    @Column(name = "tournament_id", nullable = false)
    private UUID tournamentId;

    @Column(name = "venue_id", nullable = false)
    private UUID venueId;

    @Column(name = "venue_court_id", nullable = false)
    private UUID venueCourtId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "daily_start", nullable = false)
    private LocalTime dailyStart;

    @Column(name = "daily_end", nullable = false)
    private LocalTime dailyEnd;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
