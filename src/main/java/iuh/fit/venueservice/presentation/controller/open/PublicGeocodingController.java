package iuh.fit.venueservice.presentation.controller.open;

import iuh.fit.venueservice.application.service.GeocodingService;
import iuh.fit.venueservice.presentation.response.venue.GeocodingResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/venues/geocoding")
@RequiredArgsConstructor
public class PublicGeocodingController {
    private final GeocodingService geocodingService;

    @GetMapping("/search")
    public ResponseEntity<List<GeocodingResponse>> search(
            @RequestParam @Size(min = 3, max = 160) String query
    ) {
        return ResponseEntity.ok(geocodingService.search(query).stream()
                .map(GeocodingResponse::from)
                .toList());
    }

    @GetMapping("/reverse")
    public ResponseEntity<GeocodingResponse> reverse(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude
    ) {
        return geocodingService.reverse(latitude, longitude)
                .map(GeocodingResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
