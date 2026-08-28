package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.domain.entity.Booking;
import iuh.fit.venueservice.domain.enums.BookingStatus;
import iuh.fit.venueservice.infrastructure.persistence.mapper.BookingEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaBookingRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueCourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
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
    public Optional<Booking> findByBookingCode(String bookingCode) {
        return jpaRepository.findByBookingCode(bookingCode).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findByQrCode(String qrCode) {
        return jpaRepository.findByQrCode(qrCode).map(mapper::toDomain);
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
    public long countByPlayerId(UUID playerId) {
        return jpaRepository.findByPlayerIdOrderByCreatedAtDesc(playerId, PageRequest.of(0, 1)).getTotalElements();
    }
}
