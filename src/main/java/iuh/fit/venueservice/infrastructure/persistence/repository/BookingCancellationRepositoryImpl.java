package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.BookingCancellationRepository;
import iuh.fit.venueservice.domain.entity.BookingCancellation;
import iuh.fit.venueservice.domain.enums.CancellationStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.BookingCancellationEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.BookingCancellationEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaBookingCancellationRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingCancellationRepositoryImpl implements BookingCancellationRepository {
    private final JpaBookingCancellationRepository jpaRepository;
    private final BookingCancellationEntityMapper mapper;
    private final JpaBookingRepository jpaBookingRepository;

    @Override
    public BookingCancellation save(BookingCancellation cancellation) {
        BookingCancellationEntity entity = cancellation.getCancellationId() == null
                ? mapper.toEntity(cancellation)
                : jpaRepository.findById(cancellation.getCancellationId()).map(existing -> {
                    mapper.updateEntity(cancellation, existing);
                    return existing;
                }).orElseGet(() -> mapper.toEntity(cancellation));
        entity.setBooking(jpaBookingRepository.getReferenceById(cancellation.getBookingId()));
        BookingCancellationEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<BookingCancellation> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BookingCancellation> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<BookingCancellation> findByBookingId(UUID bookingId) {
        return jpaRepository.findByBooking_BookingId(bookingId).map(mapper::toDomain);
    }

    @Override
    public Optional<BookingCancellation> findByRefundIdForUpdate(UUID refundId) {
        return jpaRepository.findByRefundIdForUpdate(refundId).map(mapper::toDomain);
    }

    @Override
    public List<BookingCancellation> findByStatus(String status, int page, int size) {
        return jpaRepository.findByStatusOrderByCreatedAtDesc(
                        CancellationStatus.valueOf(status), PageRequest.of(page, size))
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Map<UUID, BookingCancellation> findByBookingIds(Set<UUID> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) return Map.of();
        return jpaRepository.findByBooking_BookingIdIn(bookingIds).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toMap(BookingCancellation::getBookingId, cancellation -> cancellation));
    }

    @Override
    public List<BookingCancellation> findWithoutWorkflow(Set<CancellationStatus> statuses, int limit) {
        return jpaRepository.findWithoutWorkflow(statuses, PageRequest.of(0, limit)).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByRequesterAndStatusIn(UUID userId, Collection<CancellationStatus> statuses) {
        return jpaRepository.existsByRequestedByUserIdAndStatusIn(userId, statuses);
    }
}
