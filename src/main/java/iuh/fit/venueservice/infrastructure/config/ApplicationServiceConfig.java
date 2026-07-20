package iuh.fit.venueservice.infrastructure.config;

import iuh.fit.venueservice.application.port.*;
import iuh.fit.venueservice.application.port.persistence.*;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationDocumentServiceImpl;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationServiceImpl;
import iuh.fit.venueservice.application.service.impl.VenueCourtServiceImpl;
import iuh.fit.venueservice.application.service.impl.VenueServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationServiceConfig {

    @Bean
    public OwnerApplicationServiceImpl ownerApplicationService(
            OwnerApplicationRepository ownerApplicationRepository,
            VenueOwnerProfileRepository venueOwnerProfileRepository
    ) {
        return new OwnerApplicationServiceImpl(
                ownerApplicationRepository, venueOwnerProfileRepository
        );
    }

    @Bean
    public OwnerApplicationDocumentServiceImpl ownerApplicationDocumentService(
            StoragePort storagePort,
            OwnerApplicationRepository ownerApplicationRepository,
            OwnerApplicationDocumentRepository ownerApplicationDocumentRepository
    )
    {
        return new OwnerApplicationDocumentServiceImpl(
                storagePort,
                ownerApplicationRepository, ownerApplicationDocumentRepository
        );
    }

    @Bean
    public VenueServiceImpl venueService(
            VenueOwnerProfileRepository venueOwnerProfileRepository,
            VenueRepository venueRepository
    ) {
        return new VenueServiceImpl(
                venueOwnerProfileRepository, venueRepository
        );
    }

    @Bean
    public VenueCourtServiceImpl venueCourtService(
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository
    ) {
        return new VenueCourtServiceImpl(
                venueCourtRepository, venueRepository
        );
    }
}