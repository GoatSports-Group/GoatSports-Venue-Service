package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.SuggestExternalVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemand;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemandView;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueSuggestionReview;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueSuggestionReview.NearbyVenue;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.ExternalVenueNotificationPort;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueRepository;
import iuh.fit.venueservice.application.port.persistence.ExternalVenueSuggestionRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.ExternalVenueDemandService;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.ExternalVenueSuggestion;
import iuh.fit.venueservice.domain.entity.GeoBounds;
import iuh.fit.venueservice.domain.entity.GeoPoint;
import iuh.fit.venueservice.domain.enums.ExternalVenueSuggestionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ExternalVenueDemandServiceImpl implements ExternalVenueDemandService {
    private static final int MAX_PENDING_PER_PLAYER = 5;
    private static final int MAX_PAGE_SIZE = 50;
    /** Sân đã có trong khoảng này quanh vị trí gợi ý thì báo admin có thể trùng. */
    private static final double NEARBY_METERS = 150;
    /** Khung tìm sân gần: ~150 m theo vĩ độ (1° ≈ 111 km), nới rộng chút để không hụt theo kinh độ. */
    private static final double NEARBY_DEGREES = 0.002;

    private final ExternalVenueRepository externalVenueRepository;
    private final ExternalVenueSuggestionRepository suggestionRepository;
    private final VenueRepository venueRepository;
    private final ExternalVenueNotificationPort notificationPort;

    @Override
    public ExternalVenueSuggestion suggest(UUID userId, SuggestExternalVenueCommand command) throws ServiceException {
        String name = trimToNull(command.name());
        if (name == null || name.length() < 3 || name.length() > 200) {
            throw new ServiceException("Tên sân từ 3 đến 200 ký tự.");
        }
        if (command.sportTypes() == null || command.sportTypes().isEmpty()) {
            throw new ServiceException("Chọn ít nhất một môn chơi ở sân này.");
        }
        if (!new GeoBounds(command.latitude(), command.longitude(), command.latitude() + 1e-6,
                command.longitude() + 1e-6).isValid()) {
            throw new ServiceException("Vị trí sân không hợp lệ.");
        }
        if (suggestionRepository.countPendingBy(userId) >= MAX_PENDING_PER_PLAYER) {
            throw new ServiceException("Bạn đang có " + MAX_PENDING_PER_PLAYER
                    + " gợi ý chờ duyệt. Đợi admin duyệt rồi gợi ý tiếp nhé.");
        }
        return suggestionRepository.save(ExternalVenueSuggestion.builder()
                .suggestedBy(userId)
                .name(name)
                .sportTypes(EnumSet.copyOf(command.sportTypes()))
                .latitude(command.latitude())
                .longitude(command.longitude())
                .address(limit(trimToNull(command.address()), 500))
                .phone(limit(trimToNull(command.phone()), 60))
                .note(limit(trimToNull(command.note()), 500))
                .status(ExternalVenueSuggestionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Override
    public PageResult<ExternalVenueSuggestionReview> pendingSuggestions(int page, int size) {
        PageResult<ExternalVenueSuggestion> result = suggestionRepository.findByStatus(
                ExternalVenueSuggestionStatus.PENDING, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        List<ExternalVenueSuggestionReview> items = result.items().stream()
                .map(suggestion -> new ExternalVenueSuggestionReview(suggestion, nearby(suggestion)))
                .toList();
        return new PageResult<>(items, result.total(), result.page(), result.pageSize(), result.totalPages());
    }

    @Override
    public long pendingSuggestionCount() {
        return suggestionRepository.countByStatus(ExternalVenueSuggestionStatus.PENDING);
    }

    @Override
    @Transactional
    public ExternalVenue approve(UUID suggestionId, UUID adminId) throws ServiceException {
        ExternalVenueSuggestion suggestion = pending(suggestionId);
        LocalDateTime now = LocalDateTime.now();
        ExternalVenue venue = externalVenueRepository.save(ExternalVenue.builder()
                .source(ExternalVenue.SOURCE_PLAYER)
                .sourceId(suggestionId.toString())
                .name(suggestion.getName())
                .sportTypes(suggestion.getSportTypes())
                .latitude(suggestion.getLatitude())
                .longitude(suggestion.getLongitude())
                .address(suggestion.getAddress())
                .phone(suggestion.getPhone())
                .region(ExternalVenue.REGION_PLAYER)
                .importedAt(now)
                .build());
        suggestion.setStatus(ExternalVenueSuggestionStatus.APPROVED);
        suggestion.setExternalVenueId(venue.getExternalVenueId());
        suggestion.setReviewedBy(adminId);
        suggestion.setReviewedAt(now);
        suggestionRepository.save(suggestion);
        notificationPort.notifySuggester(suggestion.getSuggestedBy(), suggestionId, "Sân bạn gợi ý đã lên bản đồ",
                suggestion.getName() + " đã hiện trên bản đồ tìm sân. Cảm ơn bạn đã góp sân cho cộng đồng!");
        return venue;
    }

    @Override
    public void reject(UUID suggestionId, UUID adminId, String reason) throws ServiceException {
        ExternalVenueSuggestion suggestion = pending(suggestionId);
        String why = limit(trimToNull(reason), 500);
        suggestion.setStatus(ExternalVenueSuggestionStatus.REJECTED);
        suggestion.setRejectReason(why);
        suggestion.setReviewedBy(adminId);
        suggestion.setReviewedAt(LocalDateTime.now());
        suggestionRepository.save(suggestion);
        notificationPort.notifySuggester(suggestion.getSuggestedBy(), suggestionId, "Sân bạn gợi ý chưa được thêm",
                suggestion.getName() + (why == null ? " chưa được thêm vào bản đồ." : " chưa được thêm vào bản đồ: " + why));
    }

    @Override
    public ExternalVenueDemandView demand(UUID externalVenueId, UUID userId) throws ServiceException {
        requireVenue(externalVenueId);
        return view(externalVenueId, userId);
    }

    @Override
    public ExternalVenueDemandView want(UUID externalVenueId, UUID userId) throws ServiceException {
        requireVenue(externalVenueId);
        externalVenueRepository.addWant(externalVenueId, userId);
        return view(externalVenueId, userId);
    }

    @Override
    public ExternalVenueDemandView unwant(UUID externalVenueId, UUID userId) throws ServiceException {
        requireVenue(externalVenueId);
        externalVenueRepository.removeWant(externalVenueId, userId);
        return view(externalVenueId, userId);
    }

    @Override
    public PageResult<ExternalVenueDemand> potential(int page, int size) {
        int pageSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        int pageIndex = Math.max(page, 0);
        // ponytail: xếp và cắt trang trong bộ nhớ; chỉ gồm sân đã có nhu cầu nên còn nhỏ. Hàng chục nghìn sân thì
        // chuyển sang một truy vấn SQL có ORDER BY + LIMIT.
        List<ExternalVenueDemand> sorted = externalVenueRepository.findDemand().stream()
                .sorted(Comparator.comparingLong(ExternalVenueDemand::wantCount).reversed()
                        .thenComparing(Comparator.comparingLong(ExternalVenueDemand::matchCount).reversed())
                        .thenComparing(demand -> demand.venue().getPhone() == null)
                        .thenComparing(demand -> demand.venue().getName()))
                .toList();
        int from = Math.min(pageIndex * pageSize, sorted.size());
        int to = Math.min(from + pageSize, sorted.size());
        int pages = (int) Math.ceil(sorted.size() / (double) pageSize);
        return new PageResult<>(sorted.subList(from, to), sorted.size(), pageIndex, pageSize, pages);
    }

    @Override
    public void recordMatch(UUID sessionId, UUID externalVenueId) {
        externalVenueRepository.recordMatch(sessionId, externalVenueId);
    }

    private List<NearbyVenue> nearby(ExternalVenueSuggestion suggestion) {
        double lat = suggestion.getLatitude();
        double lng = suggestion.getLongitude();
        GeoBounds box = new GeoBounds(lat - NEARBY_DEGREES, lng - NEARBY_DEGREES, lat + NEARBY_DEGREES,
                lng + NEARBY_DEGREES);
        GeoPoint here = new GeoPoint(lat, lng);
        Stream<NearbyVenue> external = externalVenueRepository.findInBounds(box, null, null, 20).stream()
                .map(venue -> new NearbyVenue(venue.getName(),
                        (int) Math.round(here.metersTo(venue.getLatitude(), venue.getLongitude())), false));
        Stream<NearbyVenue> partners = venueRepository.findActiveLocationsIn(box).stream()
                .map(point -> new NearbyVenue("Cơ sở đối tác", (int) Math.round(here.metersTo(point.latitude(),
                        point.longitude())), true));
        return Stream.concat(external, partners)
                .filter(venue -> venue.distanceMeters() <= NEARBY_METERS)
                .sorted(Comparator.comparingInt(NearbyVenue::distanceMeters))
                .toList();
    }

    private ExternalVenueSuggestion pending(UUID suggestionId) throws ServiceException {
        ExternalVenueSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy gợi ý sân."));
        if (suggestion.getStatus() != ExternalVenueSuggestionStatus.PENDING) {
            throw new ServiceException("Gợi ý này đã được xử lý.");
        }
        return suggestion;
    }

    private void requireVenue(UUID externalVenueId) throws ServiceException {
        if (externalVenueRepository.findById(externalVenueId).isEmpty()) {
            throw new ServiceException("Không tìm thấy sân.");
        }
    }

    private ExternalVenueDemandView view(UUID externalVenueId, UUID userId) {
        return new ExternalVenueDemandView(externalVenueRepository.countWants(externalVenueId),
                userId != null && externalVenueRepository.hasWant(externalVenueId, userId));
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
