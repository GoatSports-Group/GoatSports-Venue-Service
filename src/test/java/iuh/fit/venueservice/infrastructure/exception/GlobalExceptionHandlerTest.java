package iuh.fit.venueservice.infrastructure.exception;

import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.exception.OwnershipException;
import iuh.fit.venueservice.application.exception.ResourceNotFoundException;
import iuh.fit.venueservice.application.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void technicalExceptionReturnsInternalServerError() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/owner/check-ins/walk-ins");

        ResponseEntity<BaseResponse<Object>> response = handler.handleAllExceptions(
                new IllegalStateException("No active transaction"), request
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatusCode());
    }

    @Test
    void serviceExceptionRemainsBadRequest() {
        ResponseEntity<Object> response = handler.handleServiceException(
                new ServiceException("Invalid owner application state")
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void ownershipExceptionReturnsForbidden() {
        ResponseEntity<Object> response = handler.handleOwnershipException(
                new OwnershipException("Cross-owner venue access")
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void missingResourceReturnsNotFound() {
        ResponseEntity<Object> response = handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Venue not found")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
