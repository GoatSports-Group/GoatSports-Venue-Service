package iuh.fit.venueservice.application.dto.request.venue;

import iuh.fit.venueservice.application.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateCancellationPolicyCommandTest {
    @Test
    void acceptsDescendingRefundThresholds() {
        UpdateCancellationPolicyCommand command = command(24, 12, 50.0, 0);

        assertThatCode(command::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidThresholdOrderAndNonFinitePercentage() {
        assertThatThrownBy(() -> command(12, 24, 50.0, 0).validate())
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("lớn hơn hoặc bằng");
        assertThatThrownBy(() -> command(24, 12, Double.NaN, 0).validate())
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("0 đến 100%");
    }

    @Test
    void rejectsThresholdBeyondOneYear() {
        assertThatThrownBy(() -> command(8761, 12, 50.0, 0).validate())
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("một năm");
    }

    private UpdateCancellationPolicyCommand command(
            int fullRefundHoursBefore,
            int partialRefundHoursBefore,
            double partialRefundPercentage,
            int noRefundHoursBefore
    ) {
        return new UpdateCancellationPolicyCommand(
                UUID.randomUUID(), UUID.randomUUID(), fullRefundHoursBefore,
                partialRefundHoursBefore, partialRefundPercentage, noRefundHoursBefore
        );
    }
}
