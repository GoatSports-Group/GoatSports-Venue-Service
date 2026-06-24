package iuh.fit.venueservice.infrastructure.config;

import iuh.fit.venueservice.application.port.*;
import iuh.fit.venueservice.application.port.persistence.*;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationServiceImpl;
import iuh.fit.venueservice.application.service.impl.StorageServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationServiceConfig {

    @Bean
    public StorageServiceImpl storageService(StoragePort storagePort) {
        return new StorageServiceImpl(storagePort);
    }

    @Bean
    public OwnerApplicationServiceImpl ownerApplicationService(
            OwnerApplicationRepository ownerApplicationRepository,
            VenueOwnerProfileRepository venueOwnerProfileRepository,
            StoragePort storagePort,
            UserRolePort userRolePort
    ) {
        return new OwnerApplicationServiceImpl(
                ownerApplicationRepository, venueOwnerProfileRepository, storagePort, userRolePort
        );
    }
}