package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.CheckInRecordRepository;
import iuh.fit.venueservice.domain.entity.CheckInRecord;
import iuh.fit.venueservice.infrastructure.persistence.mapper.CheckInRecordEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaBookingRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaCheckInRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import iuh.fit.venueservice.application.dto.request.venue.OwnerCheckInFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.OwnerBookingRecord;
import iuh.fit.venueservice.application.dto.response.venue.OwnerCheckInHistoryRecord;
import iuh.fit.venueservice.infrastructure.persistence.mapper.BookingEntityMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CheckInRecordRepositoryImpl implements CheckInRecordRepository {
    private final JpaCheckInRecordRepository jpaRepository;
    private final JpaBookingRepository jpaBookingRepository;
    private final CheckInRecordEntityMapper mapper;
    private final BookingEntityMapper bookingMapper;

    @Override
    public CheckInRecord save(CheckInRecord checkInRecord) {
        var entity = mapper.toEntity(checkInRecord);
        entity.setBooking(jpaBookingRepository.getReferenceById(checkInRecord.getBookingId()));
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public java.util.Optional<CheckInRecord> findByBookingId(java.util.UUID bookingId) {
        return jpaRepository.findByBooking_BookingId(bookingId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OwnerCheckInHistoryRecord> findOwnedCheckIns(OwnerCheckInFilter filter) {
        Specification<iuh.fit.venueservice.infrastructure.persistence.entity.CheckInRecordEntity> spec =
                (root, query, cb) -> cb.equal(
                        root.get("booking").get("venueCourt").get("venue").get("ownerId"), filter.ownerId()
                );
        if (filter.venueId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(
                    root.get("booking").get("venueCourt").get("venue").get("venueId"), filter.venueId()
            ));
        }
        if (filter.venueCourtId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(
                    root.get("booking").get("venueCourt").get("venueCourtId"), filter.venueCourtId()
            ));
        }
        if (filter.date() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(
                    root.get("booking").get("playDate"), filter.date()
            ));
        }
        var page = jpaRepository.findAll(spec, PageRequest.of(
                filter.page(), filter.size(), Sort.by(Sort.Direction.DESC, "checkedInAt")
        ));
        var records = page.getContent().stream().map(entity -> {
            var booking = entity.getBooking();
            var court = booking.getVenueCourt();
            var venue = court.getVenue();
            return new OwnerCheckInHistoryRecord(
                    new OwnerBookingRecord(
                            bookingMapper.toDomain(booking), venue.getVenueId(), venue.getName(), court.getName()
                    ),
                    mapper.toDomain(entity)
            );
        }).toList();
        return new PageResult<>(
                records, page.getTotalElements(), filter.page(), filter.size(), page.getTotalPages()
        );
    }
}
