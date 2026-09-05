package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.response.venue.PaymentSnapshot;
import iuh.fit.venueservice.domain.entity.Booking;

import java.util.List;

public final class BookingPaymentStatusPolicy {
    private BookingPaymentStatusPolicy() {
    }

    public static boolean isFullyPaid(Booking booking, List<PaymentSnapshot> payments) {
        if (booking.getRemainingAmount() == null || booking.getRemainingAmount() <= 0) return true;
        if (booking.getRemainingPaymentId() != null) return true;
        return payments.stream()
                .filter(payment -> "BOOKING_REMAINING".equals(payment.purpose()))
                .filter(payment -> "SUCCEEDED".equals(payment.status()))
                .anyMatch(payment -> payment.amount() != null
                        && Math.abs(payment.amount() - booking.getRemainingAmount()) < 0.01);
    }
}
