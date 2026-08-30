package iuh.fit.venueservice.infrastructure.config;

import iuh.fit.venueservice.application.port.*;
import iuh.fit.venueservice.application.port.persistence.*;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationDocumentServiceImpl;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationServiceImpl;
import iuh.fit.venueservice.application.service.impl.OwnerBookingServiceImpl;
import iuh.fit.venueservice.application.service.impl.OwnerCheckInServiceImpl;
import iuh.fit.venueservice.application.service.impl.BookingPaymentEventServiceImpl;
import iuh.fit.venueservice.application.service.impl.OwnerScheduleServiceImpl;
import iuh.fit.venueservice.application.service.impl.OwnerRevenueServiceImpl;
import iuh.fit.venueservice.application.service.impl.VenueCourtServiceImpl;
import iuh.fit.venueservice.application.service.impl.VenueServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ApplicationServiceConfig {

    @Bean
    public Clock businessClock() {
        return Clock.system(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

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
            VenueRepository venueRepository,
            StoragePort storagePort
    ) {
        return new VenueServiceImpl(
                venueRepository, storagePort
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
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository
    ) {
        return new iuh.fit.venueservice.application.service.impl.BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository,
                venueCourtRepository, venueRepository
        );
    }

    @Bean
    public OwnerScheduleServiceImpl ownerScheduleService(
            CourtPricingRuleRepository courtPricingRuleRepository,
            TimeSlotRepository timeSlotRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository
    ) {
        return new OwnerScheduleServiceImpl(
                courtPricingRuleRepository, timeSlotRepository, venueCourtRepository, venueRepository
        );
    }

    @Bean
    public OwnerBookingServiceImpl ownerBookingService(
            BookingRepository bookingRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            PaymentQueryPort paymentQueryPort
    ) {
        return new OwnerBookingServiceImpl(
                bookingRepository, venueCourtRepository, venueRepository, paymentQueryPort
        );
    }

    @Bean
    public OwnerRevenueServiceImpl ownerRevenueService(
            BookingRepository bookingRepository,
            VenueRepository venueRepository,
            PaymentQueryPort paymentQueryPort
    ) {
        return new OwnerRevenueServiceImpl(
                bookingRepository, venueRepository, paymentQueryPort
        );
    }

    @Bean
    public OwnerCheckInServiceImpl ownerCheckInService(
            BookingRepository bookingRepository,
            CheckInRecordRepository checkInRecordRepository,
            TimeSlotRepository timeSlotRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            PaymentQueryPort paymentQueryPort,
            PaymentCollectionPort paymentCollectionPort,
            BookingQrPort bookingQrPort,
            Clock clock
    ) {
        return new OwnerCheckInServiceImpl(
                bookingRepository, checkInRecordRepository, timeSlotRepository,
                venueCourtRepository, venueRepository, paymentQueryPort,
                paymentCollectionPort, bookingQrPort, clock
        );
    }

    @Bean
    public BookingPaymentEventServiceImpl bookingPaymentEventService(
            BookingRepository bookingRepository,
            BookingQrPort bookingQrPort
    ) {
        return new BookingPaymentEventServiceImpl(bookingRepository, bookingQrPort);
    }
}
