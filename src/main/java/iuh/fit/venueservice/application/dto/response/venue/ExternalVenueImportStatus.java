package iuh.fit.venueservice.application.dto.response.venue;

import java.time.LocalDateTime;

/**
 * Đợt nhập sân ngoài gần nhất, để admin thấy đang chạy tới đâu hay lỗi gì thay vì chỉ có log.
 * Một khu vực được hỏi theo từng ô nhỏ; ô lỗi (Overpass quá tải) được bỏ qua, nhập lại sau sẽ bổ sung.
 */
public record ExternalVenueImportStatus(
        String region,
        State state,
        int doneTiles,
        int totalTiles,
        int failedTiles,
        int savedCount,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
    public enum State { RUNNING, SUCCEEDED, PARTIAL, FAILED }

    public static ExternalVenueImportStatus running(String region, LocalDateTime startedAt, int doneTiles,
                                                    int totalTiles, int failedTiles, int savedCount) {
        return new ExternalVenueImportStatus(region, State.RUNNING, doneTiles, totalTiles, failedTiles, savedCount,
                startedAt, null);
    }

    public static ExternalVenueImportStatus finished(String region, LocalDateTime startedAt, int totalTiles,
                                                     int failedTiles, int savedCount) {
        State state = failedTiles == 0 ? State.SUCCEEDED : failedTiles == totalTiles ? State.FAILED : State.PARTIAL;
        return new ExternalVenueImportStatus(region, state, totalTiles, totalTiles, failedTiles, savedCount,
                startedAt, LocalDateTime.now());
    }
}
