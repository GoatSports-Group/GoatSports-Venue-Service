package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.response.venue.DemandBookingRecord;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.BookingRepository;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.OwnerDemandHistoryService;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.enums.BookingStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class OwnerDemandHistoryServiceImpl implements OwnerDemandHistoryService {
    /** Lượt thật sự giữ sân: đã cọc / đã nhận sân / đã xong, kể cả khách không đến (sân vẫn bị chiếm). */
    static final EnumSet<BookingStatus> DEMAND = EnumSet.of(
            BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED, BookingStatus.NO_SHOW);

    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final Clock clock;

    public OwnerDemandHistoryServiceImpl(BookingRepository bookingRepository, VenueRepository venueRepository,
                                         VenueCourtRepository venueCourtRepository, Clock clock) {
        this.bookingRepository = bookingRepository;
        this.venueRepository = venueRepository;
        this.venueCourtRepository = venueCourtRepository;
        this.clock = clock;
    }

    @Override
    public DemandHistory hourlyOccupancy(UUID ownerId, UUID venueId, int hours) throws ServiceException {
        if (hours < 1 || hours > MAX_HOURS) throw new ServiceException("Số giờ lịch sử phải từ 1 đến " + MAX_HOURS + ".");
        Venue venue = venueRepository.findById(venueId)
                .filter(item -> ownerId.equals(item.getOwnerId()))
                .orElseThrow(() -> new ServiceException("Không tìm thấy cơ sở của bạn."));
        int courts = (int) venueCourtRepository.findByVenueId(venueId).stream()
                .filter(court -> !Boolean.FALSE.equals(court.getActive()))
                .count();
        if (courts == 0) throw new ServiceException("Cơ sở chưa có sân hoạt động để dự báo.");

        LocalDateTime end = LocalDateTime.now(clock).truncatedTo(ChronoUnit.HOURS);
        LocalDateTime start = end.minusHours(hours);
        double[] booked = new double[hours];
        for (DemandBookingRecord booking : bookingRepository.findVenueDemandBookings(
                venueId, start.toLocalDate(), end.toLocalDate(), DEMAND)) {
            LocalDateTime from = booking.playDate().atTime(booking.startTime());
            LocalDateTime to = booking.endTime().isAfter(booking.startTime())
                    ? booking.playDate().atTime(booking.endTime())
                    : booking.playDate().plusDays(1).atTime(booking.endTime());
            addOverlap(booked, start, from, to);
        }

        // Giờ trước khi cơ sở được tạo là "chưa có dữ liệu" (null), không phải "không ai đặt" (0).
        LocalDateTime createdHour = venue.getCreatedAt() == null ? start : venue.getCreatedAt().truncatedTo(ChronoUnit.HOURS);
        List<Double> occupancy = new ArrayList<>(hours);
        for (int index = 0; index < hours; index++) {
            boolean beforeVenue = start.plusHours(index).isBefore(createdHour);
            occupancy.add(beforeVenue ? null : Math.min(1.0, round(booked[index] / courts)));
        }
        return new DemandHistory(venueId, venue.getName(), start, courts, venue.getOpenTime(), venue.getCloseTime(), occupancy);
    }

    /** Cộng phần sân-giờ một lượt đặt chiếm trong từng giờ (lượt 30 phút = 0,5 sân-giờ). */
    static void addOverlap(double[] booked, LocalDateTime windowStart, LocalDateTime from, LocalDateTime to) {
        LocalDateTime hour = from.truncatedTo(ChronoUnit.HOURS);
        while (hour.isBefore(to)) {
            long index = Duration.between(windowStart, hour).toHours();
            if (index >= 0 && index < booked.length) {
                LocalDateTime overlapStart = from.isAfter(hour) ? from : hour;
                LocalDateTime hourEnd = hour.plusHours(1);
                LocalDateTime overlapEnd = to.isBefore(hourEnd) ? to : hourEnd;
                booked[(int) index] += Duration.between(overlapStart, overlapEnd).toMinutes() / 60.0;
            }
            hour = hour.plusHours(1);
        }
    }

    private static double round(double value) {
        return Math.round(value * 10_000) / 10_000.0;
    }
}
