package iuh.fit.venueservice.infrastructure.external.client;

import iuh.fit.venueservice.infrastructure.external.config.AuthFeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "GOATSPORTS-AUTH-SERVICE",
        url = "${auth.service.url}",
        path = "/internal/users",
        configuration = AuthFeignConfiguration.class
)
public interface AuthClient {
    @PostMapping("/{userId}/roles/venue-owner")
    void assignVenueOwnerRole(@PathVariable("userId") String userId);
}
