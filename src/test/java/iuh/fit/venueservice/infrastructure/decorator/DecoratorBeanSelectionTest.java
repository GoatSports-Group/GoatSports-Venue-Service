package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.service.OwnerApplicationDocumentService;
import iuh.fit.venueservice.application.port.OwnerApplicationNotificationPort;
import iuh.fit.venueservice.application.port.OwnerApplicationBackgroundPort;
import iuh.fit.venueservice.infrastructure.external.client.NotificationClient;
import iuh.fit.venueservice.infrastructure.scheduling.VenueSemanticIndexJob;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.service.OwnerCheckInService;
import iuh.fit.venueservice.application.service.VenueCourtService;
import iuh.fit.venueservice.application.service.VenueService;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationDocumentServiceImpl;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationServiceImpl;
import iuh.fit.venueservice.application.service.impl.OwnerCheckInServiceImpl;
import iuh.fit.venueservice.application.service.impl.VenueCourtServiceImpl;
import iuh.fit.venueservice.application.service.impl.VenueServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class DecoratorBeanSelectionTest {

    @Test
    void serviceInterfacesResolveToTransactionalDecorators() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(OwnerApplicationServiceImpl.class, () -> mock(OwnerApplicationServiceImpl.class));
            context.registerBean(
                    OwnerApplicationNotificationPort.class,
                    () -> mock(OwnerApplicationNotificationPort.class)
            );
            context.registerBean(
                    OwnerApplicationBackgroundPort.class,
                    () -> mock(OwnerApplicationBackgroundPort.class)
            );
            context.registerBean(
                    OwnerApplicationDocumentServiceImpl.class,
                    () -> mock(OwnerApplicationDocumentServiceImpl.class)
            );
            context.registerBean(VenueSemanticIndexJob.class, () -> mock(VenueSemanticIndexJob.class));
            context.registerBean(
                    NotificationClient.class,
                    () -> mock(NotificationClient.class)
            );
            context.registerBean(VenueServiceImpl.class, () -> mock(VenueServiceImpl.class));
            context.registerBean(VenueCourtServiceImpl.class, () -> mock(VenueCourtServiceImpl.class));
            context.registerBean(OwnerCheckInServiceImpl.class, () -> mock(OwnerCheckInServiceImpl.class));
            context.register(
                    OwnerApplicationDecorator.class,
                    OwnerApplicationDocumentDecorator.class,
                    VenueDecorator.class,
                    VenueCourtDecorator.class,
                    OwnerCheckInDecorator.class
            );
            context.refresh();

            assertInstanceOf(OwnerApplicationDecorator.class, context.getBean(OwnerApplicationService.class));
            assertInstanceOf(
                    OwnerApplicationDocumentDecorator.class,
                    context.getBean(OwnerApplicationDocumentService.class)
            );
            assertInstanceOf(VenueDecorator.class, context.getBean(VenueService.class));
            assertInstanceOf(VenueCourtDecorator.class, context.getBean(VenueCourtService.class));
            assertInstanceOf(OwnerCheckInDecorator.class, context.getBean(OwnerCheckInService.class));
        }
    }
}
