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
            ReviewRepository reviewRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService,
            BookingQrPort bookingQrPort,
            PaymentRefundPort paymentRefundPort,
            Clock clock,
            @Value("${booking.payment-hold-duration}") Duration paymentHoldDuration,
            @Value("${booking.cancellation-auto-approval-window}") Duration cancellationAutoApprovalWindow
    ) {
        return new BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository, reviewRepository,
                venueCourtRepository, venueRepository, timeSlotLifecycleService, bookingQrPort, paymentRefundPort,
                clock, paymentHoldDuration, cancellationAutoApprovalWindow
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
            BookingCancellationRepository bookingCancellationRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            PaymentQueryPort paymentQueryPort,
            PaymentCollectionPort paymentCollectionPort,
            TimeSlotLifecycleService timeSlotLifecycleService,
            Clock clock,
            @Value("${booking.payment-checkout-duration:PT15M}") Duration paymentCheckoutDuration
    ) {
        return new OwnerBookingServiceImpl(
                bookingRepository, bookingCancellationRepository, venueCourtRepository, venueRepository, paymentQueryPort,
                paymentCollectionPort, timeSlotLifecycleService, clock, paymentCheckoutDuration
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
            TimeSlotLifecycleService timeSlotLifecycleService,
            Clock clock,
            @Value("${booking.online-check-in-early-window}") Duration onlineEarlyCheckInWindow
    ) {
        return new OwnerCheckInServiceImpl(
                bookingRepository, checkInRecordRepository, timeSlotRepository,
                venueCourtRepository, venueRepository, paymentQueryPort,
                paymentCollectionPort, bookingQrPort, timeSlotLifecycleService, clock,
                onlineEarlyCheckInWindow
        );
    }

    @Bean
    public BookingPaymentEventServiceImpl bookingPaymentEventService(
            BookingRepository bookingRepository,
            BookingQrPort bookingQrPort,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService,
            BookingCancellationRepository bookingCancellationRepository
    ) {
        return new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository
        );
    }
}
