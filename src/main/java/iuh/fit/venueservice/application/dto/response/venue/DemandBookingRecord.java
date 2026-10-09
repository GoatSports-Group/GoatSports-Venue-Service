package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDate;
import java.time.LocalTime;

/** Khung giờ một lượt đặt chiếm sân (cho chuỗi lấp đầy theo giờ). */
public record DemandBookingRecord(LocalDate playDate, LocalTime startTime, LocalTime endTime) {
}
