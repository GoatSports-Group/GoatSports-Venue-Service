package iuh.fit.venueservice.application.service.impl;

import iuh.fit.venueservice.application.dto.request.venue.SearchVenueCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.dto.response.venue.VenueSearchResponseCommand;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.port.VenueSemanticSearchPort;
import iuh.fit.venueservice.application.port.persistence.VenueCourtRepository;
import iuh.fit.venueservice.application.port.persistence.VenueRepository;
import iuh.fit.venueservice.application.service.VenueSearchService;
import iuh.fit.venueservice.domain.entity.Venue;
import iuh.fit.venueservice.domain.entity.VenueCourt;
import org.springframework.cache.annotation.Cacheable;

import java.util.Collections;
import java.util.Optional;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class VenueSearchServiceImpl implements VenueSearchService {

    /** Tran cho semantic: lay du rong de con loc tiep bang cac filter khac. */
    private static final int SEMANTIC_CANDIDATE_LIMIT = 100;

    private final VenueRepository venueRepository;
    private final VenueCourtRepository venueCourtRepository;
    private final VenueSemanticSearchPort semanticSearchPort;

    public VenueSearchServiceImpl(
            VenueRepository venueRepository,
            VenueCourtRepository venueCourtRepository,
            VenueSemanticSearchPort semanticSearchPort
    ) {
        this.venueRepository = venueRepository;
        this.venueCourtRepository = venueCourtRepository;
        this.semanticSearchPort = semanticSearchPort;
    }

    @Override
    @Cacheable(cacheNames = "venueSearch", sync = true)
    public PageResult<VenueSearchResponseCommand> searchVenues(SearchVenueCommand command) {
        PageResult<Venue> pageResult = venueRepository.search(applySemanticSearch(command));

        List<VenueSearchResponseCommand> dtoList = pageResult.items().stream().map(venue -> {
            List<VenueCourt> courts = venueCourtRepository.findByVenueId(venue.getVenueId()).stream()
                    .filter(court -> Boolean.TRUE.equals(court.getActive()))
                    .toList();

            List<String> sportTypes = courts.stream()
                    .map(c -> c.getSportType() != null ? c.getSportType().name() : null)
                    .filter(st -> st != null)
                    .distinct()
                    .collect(Collectors.toList());

            Double distance = null;
            if (command.latitude() != null && command.longitude() != null
                    && venue.getAddress() != null
                    && venue.getAddress().getLatitude() != null && venue.getAddress().getLongitude() != null) {
                distance = calculateDistance(
                        command.latitude(), command.longitude(),
                        venue.getAddress().getLatitude(), venue.getAddress().getLongitude()
                );
            }

            return new VenueSearchResponseCommand(
                    venue.getVenueId(),
                    venue.getName(),
                    venue.getDescription(),
                    venue.getOpenTime(),
                    venue.getCloseTime(),
                    venue.getMinPrice(),
                    venue.getMaxPrice(),
                    venue.getAverageRating() != null ? venue.getAverageRating() : 0.0,
                    venue.getTotalReviews() != null ? venue.getTotalReviews() : 0,
                    venue.getPhone(),
                    venue.getAddress() != null ? venue.getAddress().getAddress() : null,
                    venue.getAddress() != null ? venue.getAddress().getDistrict() : null,
                    venue.getAddress() != null ? venue.getAddress().getCity() : null,
                    venue.getAddress() != null ? venue.getAddress().getLatitude() : null,
                    venue.getAddress() != null ? venue.getAddress().getLongitude() : null,
                    venue.getImageUrls() != null ? venue.getImageUrls() : Collections.emptyList(),
                    venue.getAmenities() != null ? venue.getAmenities() : Collections.emptyList(),
                    sportTypes,
                    courts.size(),
                    distance
            );
        }).collect(Collectors.toList());

        return new PageResult<>(
                dtoList,
                pageResult.total(),
                pageResult.page(),
                pageResult.pageSize(),
                pageResult.totalPages()
        );
    }

    @Override
    public Venue getVenueDetails(UUID venueId) throws ServiceException {
        Venue venue = venueRepository.findById(venueId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cơ sở thể thao đang hoạt động."));

        List<VenueCourt> courts = getVenueCourts(venueId);
        venue.setCourts(new HashSet<>(courts));

        return venue;
    }

    @Override
    public List<VenueCourt> getVenueCourts(UUID venueId) throws ServiceException {
        Venue venue = venueRepository.findById(venueId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cơ sở thể thao đang hoạt động."));
        return venueCourtRepository.findByVenueId(venue.getVenueId()).stream()
                .filter(court -> Boolean.TRUE.equals(court.getActive()))
                .toList();
    }

    @Override
    public VenueCourt getVenueCourt(UUID venueCourtId) throws ServiceException {
        VenueCourt court = venueCourtRepository.findById(venueCourtId)
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân thi đấu đang hoạt động."));
        venueRepository.findById(court.getVenueId())
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cơ sở thể thao đang hoạt động."));
        return court;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 10.0) / 10.0;
    }

    /**
     * Khi co keyword, thu hoi semantic search truoc. AI hong hoac tat thi tra ve
     * command nguyen ban de repository dung lai keyword LIKE nhu cu.
     */
    private SearchVenueCommand applySemanticSearch(SearchVenueCommand command) {
        if (command.keyword() == null || command.keyword().trim().isEmpty()) {
            return command;
        }
        Optional<List<UUID>> semanticIds =
                semanticSearchPort.findVenueIds(command.keyword(), SEMANTIC_CANDIDATE_LIMIT);
        return semanticIds.map(command::withSemanticVenueIds).orElse(command);
    }
}
