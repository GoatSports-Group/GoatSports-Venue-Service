package iuh.fit.venueservice.infrastructure.external.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class PaymentFeignConfiguration {
    @Bean
    RequestInterceptor paymentServiceIdentity(
            @Value("${app.jwt.secret}") String gatewaySecret
    ) {
        return template -> template.header("X-Gateway-Secret", gatewaySecret);
    }
}
