package iuh.fit.venueservice.application.dto.response.venue;

/** Số người muốn đặt sân này online, và người đang xem đã bấm chưa (false khi chưa đăng nhập). */
public record ExternalVenueDemandView(long wantCount, boolean wanted) {}
