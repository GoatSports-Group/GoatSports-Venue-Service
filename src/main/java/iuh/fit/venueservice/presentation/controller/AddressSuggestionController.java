package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.service.AddressSuggestionService;
import iuh.fit.venueservice.presentation.response.venue.AddressSuggestionResponse;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/address-suggestions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AddressSuggestionController {
    private final AddressSuggestionService addressSuggestionService;

    @GetMapping
    public ResponseEntity<List<AddressSuggestionResponse>> search(
            @RequestParam @Size(min = 3, max = 160) String query
    ) {
        return ResponseEntity.ok(addressSuggestionService.search(query).stream()
                .map(AddressSuggestionResponse::from)
                .toList());
    }

    @GetMapping("/resolve")
    public ResponseEntity<AddressSuggestionResponse> resolve(
            @RequestParam @Size(min = 6, max = 2048) String refId
    ) {
        return addressSuggestionService.resolve(refId)
                .map(AddressSuggestionResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
