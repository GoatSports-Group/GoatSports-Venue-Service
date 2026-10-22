package iuh.fit.venueservice.infrastructure.external.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentFeignConfigurationTest {
    @Test
    void addsTrustedServiceHeaderToEveryPaymentCall() {
        RequestInterceptor interceptor = new PaymentFeignConfiguration().paymentServiceIdentity("service-secret");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("X-Gateway-Secret"))
                .containsExactly("service-secret");
    }
}
