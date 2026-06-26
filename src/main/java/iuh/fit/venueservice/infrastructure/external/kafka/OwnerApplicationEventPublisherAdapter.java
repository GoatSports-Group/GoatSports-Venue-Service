package iuh.fit.venueservice.infrastructure.external.kafka;

import iuh.fit.venueservice.application.dto.event.OwnerApplicationEvent;
import iuh.fit.venueservice.application.dto.event.type.OwnerApplicationEventType;
import iuh.fit.venueservice.application.port.event.OwnerApplicationEventPublisher;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerApplicationEventPublisherAdapter implements OwnerApplicationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.owner-application}")
    private String topic;

    @Override
    public void publishOwnerApplicationSubmittedAfterCommit(OwnerApplication ownerApplication) {
        OwnerApplicationEvent event = toOwnerApplicationEvent(ownerApplication, OwnerApplicationEventType.SUBMITTED);
        if(event == null) {
            return;
        }

        if(TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publishOwnerApplicationEvent(ownerApplication.getUserId().toString(), event);
                    }
                }
            );
        } else {
            publishOwnerApplicationEvent(ownerApplication.getUserId().toString(), event);
        }
    }

    @Override
    public void publishOwnerApplicationRejectedAfterCommit(OwnerApplication ownerApplication) {

    }

    @Override
    public void publishOwnerApplicationApprovedAfterCommit(OwnerApplication ownerApplication) {

    }

    @Override
    public void publishOwnerApplicationEvent(String userId, OwnerApplicationEvent event) {
        kafkaTemplate.send(topic, userId, event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Gửi [{}] thành công cho User: {}", event.getEventType().name(), userId);
                } else {
                    log.error("Thất bại khi gửi [{}] vì: {}", event.getEventType().name(), ex.getMessage());
                }
            });
    }

    private OwnerApplicationEvent toOwnerApplicationEvent(
            OwnerApplication ownerApplication, OwnerApplicationEventType eventType
    )
    {
        if (ownerApplication == null) return null;

        return OwnerApplicationEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .data(OwnerApplicationEvent.OwnerApplicationData.builder()
                        .ownerApplicationId(ownerApplication.getOwnerApplicationId())
                        .userId(ownerApplication.getUserId())
                        .email(ownerApplication.getEmail())
                        .status(ownerApplication.getStatus().name())
                        .rejectReason(ownerApplication.getRejectReason())
                        .build())
                .build();
    }

}
