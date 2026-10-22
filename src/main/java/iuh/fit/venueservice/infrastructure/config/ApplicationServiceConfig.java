package iuh.fit.venueservice.infrastructure.config;

import iuh.fit.venueservice.application.port.*;
import iuh.fit.venueservice.application.port.persistence.CourtHoldRepository;
import iuh.fit.venueservice.application.port.persistence.*;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
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
            OwnerApplicationDocumentRepository ownerApplicationDocumentRepository,
            VenueRepository venueRepository
    ) {
        return new OwnerApplicationServiceImpl(
                ownerApplicationRepository, ownerApplicationDocumentRepository, venueRepository
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
    public OwnerApplicationRegistrationServiceImpl ownerApplicationRegistrationService(
            OwnerApplicationService ownerApplicationService,
            OwnerApplicationRepository ownerApplicationRepository,
            StoragePort storagePort,
            IdentityVerificationPort identityVerificationPort
    ) {
        return new OwnerApplicationRegistrationServiceImpl(
                ownerApplicationService,
                ownerApplicationRepository,
                storagePort,
                identityVerificationPort
        );
    }

    @Bean
    public VenueServiceImpl venueService(
            VenueRepository venueRepository,
            CancellationPolicyRepository cancellationPolicyRepository,
            StoragePort storagePort
    ) {
        return new VenueServiceImpl(
                venueRepository, cancellationPolicyRepository, storagePort
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
            VenueCourtRepository venueCourtRepository,
            VenueSemanticSearchPort semanticSearchPort,
            ActivityEventPublisher activityEventPublisher
    ) {
        return new VenueSearchServiceImpl(
                venueRepository, venueCourtRepository, semanticSearchPort, activityEventPublisher
        );
    }

    @Bean
    public GeocodingServiceImpl geocodingService(GeocodingPort geocodingPort) {
        return new GeocodingServiceImpl(geocodingPort);
    }

    @Bean
    public AddressSuggestionServiceImpl addressSuggestionService(AddressSuggestionPort addressSuggestionPort) {
        return new AddressSuggestionServiceImpl(addressSuggestionPort);
    }

    @Bean
    public BookingServiceImpl bookingService(
            BookingRepository bookingRepository,
            TimeSlotRepository timeSlotRepository,
            BookingCancellationRepository bookingCancellationRepository,
            ReviewRepository reviewRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            BookingQrPort bookingQrPort,
            Clock clock,
            @Value("${booking.payment-hold-duration}") Duration paymentHoldDuration,
            CourtHoldRepository courtHoldRepository
    ) {
        return new BookingServiceImpl(
                bookingRepository, timeSlotRepository, bookingCancellationRepository, reviewRepository,
                venueCourtRepository, venueRepository, bookingQrPort, clock, paymentHoldDuration, courtHoldRepository
        );
    }

    @Bean
    public BookingCancellationServiceImpl bookingCancellationService(
            BookingRepository bookingRepository,
            BookingCancellationRepository bookingCancellationRepository,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService,
            PaymentRefundPort paymentRefundPort,
            MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort,
            BookingNotificationPort bookingNotificationPort,
            CancellationWorkflowPort cancellationWorkflowPort,
            Clock clock,
            @Value("${booking.cancellation-auto-approval-window}") Duration cancellationAutoApprovalWindow
    ) {
        return new BookingCancellationServiceImpl(
                bookingRepository, bookingCancellationRepository, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, paymentRefundPort, matchmakingLifecycleSyncPort, bookingNotificationPort,
                cancellationWorkflowPort, clock, cancellationAutoApprovalWindow
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
            VenueRepository venueRepository,
            CourtHoldRepository courtHoldRepository
    ) {
        return new OwnerScheduleServiceImpl(
                courtPricingRuleRepository, timeSlotRepository, venueCourtRepository, venueRepository, courtHoldRepository
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
    public OwnerDemandHistoryServiceImpl ownerDemandHistoryService(
            BookingRepository bookingRepository,
            VenueRepository venueRepository,
            VenueCourtRepository venueCourtRepository,
            Clock clock
    ) {
        return new OwnerDemandHistoryServiceImpl(
                bookingRepository, venueRepository, venueCourtRepository, clock);
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
            @Value("${booking.online-check-in-early-window}") Duration onlineEarlyCheckInWindow,
            MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort,
            CourtHoldRepository courtHoldRepository
    ) {
        return new OwnerCheckInServiceImpl(
                bookingRepository, checkInRecordRepository, timeSlotRepository,
                venueCourtRepository, venueRepository, paymentQueryPort,
                paymentCollectionPort, bookingQrPort, timeSlotLifecycleService, clock,
                onlineEarlyCheckInWindow, matchmakingLifecycleSyncPort, courtHoldRepository
        );
    }

    @Bean
    public BookingPaymentEventServiceImpl bookingPaymentEventService(
            BookingRepository bookingRepository,
            BookingQrPort bookingQrPort,
            VenueCourtRepository venueCourtRepository,
            VenueRepository venueRepository,
            TimeSlotLifecycleService timeSlotLifecycleService,
            BookingCancellationRepository bookingCancellationRepository,
            MatchmakingLifecycleSyncPort matchmakingLifecycleSyncPort,
            PaymentRefundPort paymentRefundPort,
            BookingNotificationPort bookingNotificationPort
    ) {
        return new BookingPaymentEventServiceImpl(
                bookingRepository, bookingQrPort, venueCourtRepository, venueRepository,
                timeSlotLifecycleService, bookingCancellationRepository, matchmakingLifecycleSyncPort, paymentRefundPort,
                bookingNotificationPort
        );
    }

    @Bean
    public AccountDataServiceImpl accountDataService(
            BookingRepository bookingRepository,
            BookingCancellationRepository cancellationRepository,
            VenueRepository venueRepository,
            OwnerApplicationRepository ownerApplicationRepository,
            StoragePort storagePort) {
        return new AccountDataServiceImpl(
                bookingRepository, cancellationRepository, venueRepository, ownerApplicationRepository, storagePort);
    }
}
