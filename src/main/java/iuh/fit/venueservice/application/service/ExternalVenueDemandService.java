package iuh.fit.venueservice.application.service;

import iuh.fit.venueservice.application.dto.request.venue.SuggestExternalVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemand;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueDemandView;
import iuh.fit.venueservice.application.dto.response.venue.ExternalVenueSuggestionReview;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.entity.ExternalVenue;
import iuh.fit.venueservice.domain.entity.ExternalVenueSuggestion;

import java.util.UUID;

/**
 * Nhu cầu với sân ngoài hệ thống (cold start, giai đoạn 2): người chơi gợi ý sân còn thiếu, bấm "muốn đặt online",
 * kèo hoàn tất ở sân được đếm; admin dùng các con số đó để chọn sân nên mời làm đối tác.
 */
public interface ExternalVenueDemandService {
    ExternalVenueSuggestion suggest(UUID userId, SuggestExternalVenueCommand command) throws ServiceException;

    PageResult<ExternalVenueSuggestionReview> pendingSuggestions(int page, int size);

    long pendingSuggestionCount();

    /** Tạo sân ngoài nguồn PLAYER từ lượt gợi ý và báo người gợi ý. */
    ExternalVenue approve(UUID suggestionId, UUID adminId) throws ServiceException;

    void reject(UUID suggestionId, UUID adminId, String reason) throws ServiceException;

    /** {@code userId} null khi người xem chưa đăng nhập. */
    ExternalVenueDemandView demand(UUID externalVenueId, UUID userId) throws ServiceException;

    ExternalVenueDemandView want(UUID externalVenueId, UUID userId) throws ServiceException;

    ExternalVenueDemandView unwant(UUID externalVenueId, UUID userId) throws ServiceException;

    /** Sân có người muốn đặt hoặc đã có kèo: nhiều lượt muốn đặt trước, rồi nhiều kèo, rồi sân có số điện thoại. */
    PageResult<ExternalVenueDemand> potential(int page, int size);

    /** ai-service báo một kèo ghép trận đã hoàn tất ở sân ngoài; báo lại cùng phiên không đếm trùng. */
    void recordMatch(UUID sessionId, UUID externalVenueId);
}
