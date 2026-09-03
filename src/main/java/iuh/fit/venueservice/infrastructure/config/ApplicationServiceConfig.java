package iuh.fit.venueservice.infrastructure.config;

import iuh.fit.venueservice.application.port.*;
import iuh.fit.venueservice.application.port.persistence.*;
import iuh.fit.venueservice.application.service.impl.*;
import iuh.fit.venueservice.application.service.TimeSlotLifecycleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.Duration;
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
            VenueRepository venueRepository,
            TimeSlotRepository timeSlotRepository,
            Clock clock
    ) {
        return new VenueCourtServiceImpl(
                venueCourtRepository, venueRepository, timeSlotRepository, clock
        );
    }

    @Bean
    public VenueSearchServiceImpl venueSearchService(
            VenueRepository venueRepository,
            VenueCourtRepository venueCourtRepository
    ) {
        return new VenueSearchServiceImpl(
                venueRepository, venueCourtRepository
        );
    }

    @Bean
    public BookingServiceImpl bookingService(
            BookingRepository bookingRepository,
            TimeSlotRepository timeSlotRepository,
            BookingCancellationRepository bookingCancellationRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService,
            Clock clock,
            @Value("${booking.payment-hold-duration}") Duration paymentHoldDuration
    ) {
        return new BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository,
                venueCourtRepository, venueRepository, timeSlotLifecycleService, clock, paymentHoldDuration
        );
    }

    @Bean
    public TimeSlotLifecycleService timeSlotLifecycleService(
            TimeSlotRepository timeSlotRepository,
            Clock clock
    ) {
        return new TimeSlotLifecycleService(timeSlotRepository, clock);
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
            PaymentQueryPort paymentQueryPort,
            PaymentCollectionPort paymentCollectionPort,
            TimeSlotLifecycleService timeSlotLifecycleService,
            Clock clock
    ) {
        return new OwnerBookingServiceImpl(
                bookingRepository, venueCourtRepository, venueRepository, paymentQueryPort,
                paymentCollectionPort, timeSlotLifecycleService, clock
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
    public ReviewServiceImpl reviewService(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository
    ) {
        return new ReviewServiceImpl(
                reviewRepository, bookingRepository, venueCourtRepository, venueRepository
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
            Clock clock,
            @Value("${booking.payment-hold-duration}") Duration paymentHoldDuration,
            @Value("${booking.online-check-in-early-window}") Duration onlineEarlyCheckInWindow
    ) {
        return new OwnerCheckInServiceImpl(
                bookingRepository, checkInRecordRepository, timeSlotRepository,
                venueCourtRepository, venueRepository, paymentQueryPort,
                paymentCollectionPort, bookingQrPort, clock, paymentHoldDuration,
                onlineEarlyCheckInWindow
        );
    }

    @Bean
    public BookingPaymentEventServiceImpl bookingPaymentEventService(
            BookingRepository bookingRepository,
            BookingQrPort bookingQrPort,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService
    ) {
        return new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService
        );
    }
}
