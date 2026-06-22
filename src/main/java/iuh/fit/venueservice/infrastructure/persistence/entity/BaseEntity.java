package iuh.fit.venueservice.infrastructure.persistence.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public class BaseEntity {
    @CreationTimestamp
    private LocalDateTime createdAt;
    @CreatedBy
    private String createdBy;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    @LastModifiedBy
    private String updatedBy;
}
