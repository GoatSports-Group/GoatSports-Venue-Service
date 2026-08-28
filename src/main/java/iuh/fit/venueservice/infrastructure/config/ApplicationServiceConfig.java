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
            VenueRepository venueRepository
    ) {
        return new OwnerApplicationServiceImpl(
                ownerApplicationRepository, venueRepository
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
            VenueRepository venueRepository
    ) {
        return new VenueServiceImpl(
                venueRepository
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

    @Bean
    public iuh.fit.venueservice.application.service.impl.VenueSearchServiceImpl venueSearchService(
            VenueRepository venueRepository,
            VenueCourtRepository venueCourtRepository
    ) {
        return new iuh.fit.venueservice.application.service.impl.VenueSearchServiceImpl(
                venueRepository, venueCourtRepository
        );
    }

    @Bean
    public iuh.fit.venueservice.application.service.impl.BookingServiceImpl bookingService(
            BookingRepository bookingRepository,
            TimeSlotRepository timeSlotRepository,
            BookingCancellationRepository bookingCancellationRepository,
            CheckInRecordRepository checkInRecordRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository
    ) {
        return new iuh.fit.venueservice.application.service.impl.BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository,
                checkInRecordRepository, venueCourtRepository, venueRepository
        );
    }
}
