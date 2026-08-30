package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.presentation.controller.admin.VenueAdminController;
import iuh.fit.venueservice.presentation.controller.admin.VenueCourtAdminController;
import iuh.fit.venueservice.presentation.controller.open.PublicVenueController;
import iuh.fit.venueservice.presentation.controller.open.PublicVenueCourtController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class VenueControllerPermissionContractTest {

    @Test
    void ownerAndAdminMethodsUsePermissionsInsteadOfRoleOnlyChecks() {
        assertPermission(OwnerVenueController.class, "getMyVenues", "GET_OWN_VENUES");
        assertPermission(OwnerVenueController.class, "getOwnedVenue", "GET_OWN_VENUE");
        assertPermission(OwnerVenueController.class, "updateVenue", "UPDATE_OWN_VENUE");
        assertPermission(OwnerVenueCourtController.class, "getVenueCourts", "GET_OWN_VENUE_COURTS");
        assertPermission(OwnerVenueCourtController.class, "createVenueCourt", "CREATE_OWN_VENUE_COURT");
        assertPermission(OwnerVenueCourtController.class, "getVenueCourt", "GET_OWN_VENUE_COURT");
        assertPermission(OwnerVenueCourtController.class, "updateVenueCourt", "UPDATE_OWN_VENUE_COURT");
        assertPermission(OwnerVenueCourtController.class, "updateVenueCourtActive", "CHANGE_OWN_VENUE_COURT_STATUS");
        assertPermission(OwnerScheduleController.class, "getPricingRules", "GET_OWN_COURT_PRICING_RULES");
        assertPermission(OwnerScheduleController.class, "createPricingRule", "CREATE_OWN_COURT_PRICING_RULE");
        assertPermission(OwnerScheduleController.class, "updatePricingRule", "UPDATE_OWN_COURT_PRICING_RULE");
        assertPermission(OwnerScheduleController.class, "deletePricingRule", "DELETE_OWN_COURT_PRICING_RULE");
        assertPermission(OwnerScheduleController.class, "getTimeSlots", "GET_OWN_TIME_SLOTS");
        assertPermission(OwnerScheduleController.class, "generateTimeSlots", "CREATE_OWN_TIME_SLOTS");
        assertPermission(OwnerScheduleController.class, "updateTimeSlotStatus", "UPDATE_OWN_TIME_SLOT_STATUS");
        assertPermission(OwnerScheduleController.class, "deleteTimeSlot", "DELETE_OWN_TIME_SLOT");
        assertPermission(OwnerBookingController.class, "getBookings", "GET_OWN_BOOKINGS");
        assertPermission(OwnerBookingController.class, "getBooking", "GET_OWN_BOOKING");
        assertPermission(OwnerBookingController.class, "updateStatus", "UPDATE_OWN_BOOKING_STATUS");
        assertPermission(VenueAdminController.class, "getVenues", "GET_ALL_VENUES");
        assertPermission(VenueAdminController.class, "getVenue", "GET_ANY_VENUE");
        assertPermission(VenueCourtAdminController.class, "getVenueCourts", "GET_ANY_VENUE_COURTS");
        assertPermission(VenueCourtAdminController.class, "getVenueCourt", "GET_ANY_VENUE_COURT");
    }

    @Test
    void publicReadControllersDoNotRequirePermission() {
        assertThat(PublicVenueController.class.getAnnotation(PreAuthorize.class)).isNull();
        assertThat(PublicVenueCourtController.class.getAnnotation(PreAuthorize.class)).isNull();
        assertThat(Arrays.stream(PublicVenueController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PreAuthorize.class))
                .allMatch(annotation -> annotation == null)).isTrue();
        assertThat(Arrays.stream(PublicVenueCourtController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(PreAuthorize.class))
                .allMatch(annotation -> annotation == null)).isTrue();
    }

    private void assertPermission(Class<?> controller, String methodName, String permission) {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasAuthority('" + permission + "')");
    }
}
