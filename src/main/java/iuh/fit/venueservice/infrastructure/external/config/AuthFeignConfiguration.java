package iuh.fit.venueservice.infrastructure.external.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class AuthFeignConfiguration {
    @Bean
    RequestInterceptor authServiceIdentity(
            @Value("${app.gateway.secret}") String gatewaySecret
    ) {
        return template -> template.header("X-Gateway-Secret", gatewaySecret);
    }
}
