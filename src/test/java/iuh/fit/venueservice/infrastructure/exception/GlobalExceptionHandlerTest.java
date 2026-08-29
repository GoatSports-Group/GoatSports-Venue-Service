package iuh.fit.venueservice.infrastructure.exception;

import iuh.fit.venueservice.application.dto.response.base.BaseResponse;
import iuh.fit.venueservice.application.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void technicalExceptionReturnsInternalServerError() {
        ResponseEntity<BaseResponse<Object>> response = handler.handleAllExceptions(
                new IllegalStateException("No active transaction")
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
}
