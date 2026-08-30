package iuh.fit.venueservice.infrastructure.external.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.service.BookingPaymentEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {
    @Mock private BookingPaymentEventService bookingPaymentEventService;

    @Test
    void mapsThePublishedPaymentSucceededEnvelopeToTheVenueUseCase() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        var consumer = new PaymentEventConsumer(new ObjectMapper(), bookingPaymentEventService);

        consumer.consume("""
                {
                  "eventType": "payment.succeeded.v1",
                  "payload": {
                    "paymentId": "%s",
                    "payerId": "%s",
                    "purpose": "BOOKING_DEPOSIT",
                    "referenceType": "BOOKING",
                    "referenceId": "%s",
                    "amount": 60000.0,
                    "currency": "VND",
                    "status": "SUCCEEDED"
                  }
                }
                """.formatted(paymentId, payerId, bookingId));

        ArgumentCaptor<PaymentSucceededCommand> command =
                ArgumentCaptor.forClass(PaymentSucceededCommand.class);
        verify(bookingPaymentEventService).applySucceededPayment(command.capture());
        assertThat(command.getValue()).isEqualTo(new PaymentSucceededCommand(
                paymentId, payerId, "BOOKING_DEPOSIT", "BOOKING",
                bookingId, 60_000.0, "VND", "SUCCEEDED"
        ));
    }

    @Test
    void ignoresOtherEventTypes() throws Exception {
        var consumer = new PaymentEventConsumer(new ObjectMapper(), bookingPaymentEventService);

        consumer.consume("{\"eventType\":\"payment.failed.v1\",\"payload\":{}}");

        verify(bookingPaymentEventService, never()).applySucceededPayment(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void rejectsMalformedSucceededEventsSoKafkaCanRetryThem() {
        var consumer = new PaymentEventConsumer(new ObjectMapper(), bookingPaymentEventService);

        assertThatThrownBy(() -> consumer.consume("""
                {"eventType":"payment.succeeded.v1","payload":{"paymentId":"bad-uuid"}}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contract");
    }
}
