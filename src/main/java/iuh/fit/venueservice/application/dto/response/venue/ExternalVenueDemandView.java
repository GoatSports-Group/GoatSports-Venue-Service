package iuh.fit.venueservice.application.dto.response.venue;

/**
 * Số người muốn đặt sân này online, người đang xem đã bấm chưa (false khi chưa đăng nhập), và số kèo ghép trận đã chơi
 * ở sân (trang lời mời cho chủ sân xem).
 */
public record ExternalVenueDemandView(long wantCount, boolean wanted, long matchCount) {}
