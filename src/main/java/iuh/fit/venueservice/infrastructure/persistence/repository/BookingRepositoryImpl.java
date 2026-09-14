package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.dto.request.venue.OwnerBookingFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingRecord;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCustomerMetricsRecord;
import iuh.fit.venueservice.application.dto.response.venue.RevenueBookingRecord;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingSource;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.BookingEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.BookingEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaBookingRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueCourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookingRepositoryImpl implements BookingRepository {
    private final JpaBookingRepository jpaRepository;
    private final BookingEntityMapper mapper;
    private final JpaVenueCourtRepository jpaVenueCourtRepository;

    @Override
    public Booking save(Booking booking) {
        var entity = booking.getBookingId() == null
                ? mapper.toEntity(booking)
                : jpaRepository.findById(booking.getBookingId()).map(existing -> {
                    mapper.updateEntity(booking, existing);
                    return existing;
                }).orElseGet(() -> mapper.toEntity(booking));
        entity.setVenueCourt(jpaVenueCourtRepository.getReferenceById(booking.getVenueCourtId()));
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<Booking> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findByBookingCode(String bookingCode) {
        return jpaRepository.findByBookingCode(bookingCode).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findByQrCode(String qrCode) {
        return jpaRepository.findByQrCode(qrCode).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findLatestByMatchmakingSessionId(UUID matchmakingSessionId) {
        return jpaRepository.findFirstByMatchmakingSessionIdOrderByCreatedAtDesc(matchmakingSessionId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Booking> findByPlayerId(UUID playerId, int page, int size) {
        return jpaRepository.findByPlayerIdOrderByCreatedAtDesc(playerId, PageRequest.of(page, size))
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Booking> findByPlayerIdAndStatus(UUID playerId, String status, int page, int size) {
        return jpaRepository.findByPlayerIdAndStatusOrderByCreatedAtDesc(
                        playerId, BookingStatus.valueOf(status), PageRequest.of(page, size))
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Booking> findPlayerPage(UUID playerId, BookingStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = status == null
                ? jpaRepository.findByPlayerId(playerId, pageable)
                : jpaRepository.findByPlayerIdAndStatus(playerId, status, pageable);
        return new PageResult<>(
                result.getContent().stream().map(mapper::toDomain).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize(), result.getTotalPages()
        );
    }

    @Override
    public long countByPlayerId(UUID playerId) {
        return jpaRepository.findByPlayerIdOrderByCreatedAtDesc(playerId, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    @Transactional
    public List<Booking> findExpiredPaymentHoldsForUpdate(LocalDateTime cutoff) {
        return jpaRepository.findExpiredPaymentHoldsForUpdate(
                        cutoff, BookingStatus.PENDING_PAYMENT, BookingSource.WALK_IN
                ).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<Booking> findCompletionCandidatesForUpdate(LocalDateTime cutoff, int limit) {
        return jpaRepository.findCompletionCandidatesForUpdate(
                        cutoff.toLocalDate(),
                        cutoff.toLocalTime(),
                        BookingStatus.CHECKED_IN,
                        PageRequest.of(0, limit)
                ).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerBookingRecord> findOwnedBookings(OwnerBookingFilter filter) {
        Specification<BookingEntity> spec =
                (root, query, cb) -> cb.equal(
                        root.get("venueCourt").get("venue").get("ownerId"), filter.ownerId()
                );
        if (filter.venueId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(
                    root.get("venueCourt").get("venue").get("venueId"), filter.venueId()
            ));
        }
        if (filter.venueCourtId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(
                    root.get("venueCourt").get("venueCourtId"), filter.venueCourtId()
            ));
        }
        if (filter.status() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.fromDate() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(
                    root.get("playDate"), filter.fromDate()
            ));
        }
        if (filter.toDate() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(
                    root.get("playDate"), filter.toDate()
            ));
        }
        if (filter.query() != null && !filter.query().isBlank()) {
            String term = "%" + filter.query().trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("bookingCode")), term),
                    cb.like(cb.lower(root.get("walkInCustomerName")), term),
                    cb.like(cb.lower(root.get("walkInCustomerPhone")), term)
            ));
        }

        var page = jpaRepository.findAll(
                spec,
                PageRequest.of(filter.page(), filter.size(), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<OwnerBookingRecord> records = page.getContent().stream()
                .map(this::toOwnerRecord)
                .toList();
        return new PageResult<>(records, page.getTotalElements(), filter.page(), filter.size(), page.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OwnerBookingRecord> findOwnedBookingById(UUID bookingId, UUID ownerId) {
        return jpaRepository.findById(bookingId)
                .filter(entity -> ownerId.equals(entity.getVenueCourt().getVenue().getOwnerId()))
                .map(this::toOwnerRecord);
    }

    @Override
    @Transactional
    public Optional<OwnerBookingRecord> findOwnedBookingByIdForUpdate(UUID bookingId, UUID ownerId) {
        return jpaRepository.findOwnedByIdForUpdate(bookingId, ownerId).map(this::toOwnerRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueBookingRecord> findOwnedRevenueBookings(
            UUID ownerId, UUID venueId, LocalDate fromDate, LocalDate toDate
    ) {
        return jpaRepository.findOwnedForRevenue(ownerId, venueId, fromDate, toDate).stream()
                .map(entity -> new RevenueBookingRecord(
                        entity.getBookingId(), entity.getPlayDate(), entity.getStartTime()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerCustomerMetricsRecord findOwnedCustomerMetrics(
            UUID ownerId,
            UUID venueId,
            LocalDate previousFrom,
            LocalDate currentFrom,
            LocalDate currentToExclusive
    ) {
        var row = jpaRepository.findOwnedCustomerMetrics(
                ownerId, venueId, previousFrom, currentFrom, currentToExclusive
        );

        return new OwnerCustomerMetricsRecord(
                number(row.getCurrentTotalCustomers()),
                number(row.getCurrentNewCustomers()),
                number(row.getCurrentReturningCustomers()),
                number(row.getPreviousTotalCustomers()),
                number(row.getPreviousNewCustomers()),
                number(row.getPreviousReturningCustomers())
        );
    }

    private long number(Number value) {
        return value == null ? 0 : value.longValue();
    }

    private OwnerBookingRecord toOwnerRecord(
            BookingEntity entity
    ) {
        var court = entity.getVenueCourt();
        var venue = court.getVenue();
        return new OwnerBookingRecord(
                mapper.toDomain(entity), venue.getVenueId(), venue.getName(), court.getName()
        );
    }
}
