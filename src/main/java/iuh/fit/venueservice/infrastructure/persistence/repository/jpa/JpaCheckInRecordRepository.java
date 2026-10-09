package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.CheckInRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCheckInRecordRepository extends JpaRepository<CheckInRecordEntity, UUID>,
        JpaSpecificationExecutor<CheckInRecordEntity> {
    Optional<CheckInRecordEntity> findByBooking_BookingId(UUID bookingId);

    // Lịch sử check-in đọc lượt đặt, sân, cơ sở của từng dòng; các @OneToOne(mappedBy) (hủy / đánh giá của lượt,
    // chính sách hủy của cơ sở) không lazy được nên phải join luôn, không thì mỗi dòng thêm vài truy vấn (N+1).
    @Override
    @EntityGraph(attributePaths = {"booking", "booking.venueCourt", "booking.venueCourt.venue",
            "booking.venueCourt.venue.cancellationPolicy", "booking.cancellation", "booking.review"})
    Page<CheckInRecordEntity> findAll(Specification<CheckInRecordEntity> spec, Pageable pageable);
}
