package iuh.fit.venueservice.application.port;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface BookingTicketMailPort {
    void sendTicket(UUID playerId, BookingTicket ticket);

    record BookingTicket(UUID bookingId, String bookingCode, String venueName, String courtName, String venueAddress,
                         LocalDate playDate, LocalTime startTime, LocalTime endTime,
                         Double depositAmount, Double remainingAmount, String qrCode) {
    }
}
