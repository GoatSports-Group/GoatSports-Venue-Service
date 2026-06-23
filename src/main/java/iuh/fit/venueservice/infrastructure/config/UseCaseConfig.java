package iuh.fit.venueservice.infrastructure.config;

import iuh.fit.venueservice.application.port.*;
import iuh.fit.venueservice.application.port.persistence.*;
import iuh.fit.venueservice.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public StorageUseCase storageUseCase(StoragePort storagePort) {
        return new StorageUseCase(storagePort);
    }

    @Bean
    public OwnerApplicationUseCase ownerApplicationUseCase(
            OwnerApplicationRepository ownerApplicationRepository,
            VenueOwnerProfileRepository venueOwnerProfileRepository,
            StoragePort storagePort,
            UserRolePort userRolePort
    ) {
        return new OwnerApplicationUseCase(
                ownerApplicationRepository, venueOwnerProfileRepository, storagePort, userRolePort
        );
    }
}
