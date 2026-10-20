package iuh.fit.venueservice.infrastructure.external.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.venueservice.application.dto.request.payment.PaymentSucceededCommand;
import iuh.fit.venueservice.application.dto.request.payment.PaymentTerminalCommand;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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

        consumer.consume("{\"eventType\":\"payment.processing.v1\",\"payload\":{}}");

        verify(bookingPaymentEventService, never()).applySucceededPayment(
                org.mockito.ArgumentMatchers.any()
        );
        verify(bookingPaymentEventService, never()).applyTerminalPayment(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void mapsExpiredPaymentToTheVenueUseCase() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        var consumer = new PaymentEventConsumer(new ObjectMapper(), bookingPaymentEventService);

        consumer.consume("""
                {
                  "eventType": "payment.expired.v1",
                  "payload": {
                    "paymentId": "%s",
                    "payerId": "%s",
                    "purpose": "BOOKING_DEPOSIT",
                    "referenceType": "BOOKING",
                    "referenceId": "%s",
                    "status": "EXPIRED",
                    "failureCode": "PAYMENT_EXPIRED",
                    "failureReason": "Đã hết thời gian giữ chỗ thanh toán"
                  }
                }
                """.formatted(paymentId, payerId, bookingId));

        ArgumentCaptor<PaymentTerminalCommand> command =
                ArgumentCaptor.forClass(PaymentTerminalCommand.class);
        verify(bookingPaymentEventService).applyTerminalPayment(command.capture());
        assertThat(command.getValue()).isEqualTo(new PaymentTerminalCommand(
                paymentId, payerId, "BOOKING_DEPOSIT", "BOOKING", bookingId,
                "EXPIRED", "PAYMENT_EXPIRED", "Đã hết thời gian giữ chỗ thanh toán"
        ));
    }

    @Test
    void leavesRefundEventsToTheCancellationWorkflow() throws Exception {
        var consumer = new PaymentEventConsumer(new ObjectMapper(), bookingPaymentEventService);

        consumer.consume("""
                {
                  "eventType": "refund.failed.v1",
                  "payload": {
                    "refundId": "%s",
                    "paymentId": "%s",
                    "status": "FAILED",
                    "failureReason": "Không tìm thấy tài khoản nhận chi"
                  }
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));

        verifyNoInteractions(bookingPaymentEventService);
    }

    @Test
    void refundReminderIsNotMistakenForARefundResultAndOnlyCashRefundsRemindTheOwner() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        var consumer = new PaymentEventConsumer(new ObjectMapper(), bookingPaymentEventService);
        String reminder = """
                {
                  "eventId": "%s",
                  "eventType": "reminder.refund-pending.v1",
                  "payload": { "refundId": "%s", "amount": 60000.0, "status": "%s" }
                }
                """;

        consumer.consume(reminder.formatted(eventId, refundId, "MANUAL_REVIEW"));
        consumer.consume(reminder.formatted(UUID.randomUUID(), refundId, "AWAITING_CLAIM"));

        verify(bookingPaymentEventService).remindOwnerOfManualRefund(refundId, eventId, 60_000.0);
        verifyNoMoreInteractions(bookingPaymentEventService);
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
