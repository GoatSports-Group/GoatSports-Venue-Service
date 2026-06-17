package iuh.fit.venueservice.entity;

import iuh.fit.venueservice.service.JwtService;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public class BaseEntity {
    @CreationTimestamp
    private LocalDateTime createdAt;
    private String createdBy;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private String updatedBy;

    @PrePersist
    public void onCreate() {
        this.createdBy = JwtService.getCurrentUser().isPresent()
                ? JwtService.getCurrentUser().get()
                : "System";
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedBy = JwtService.getCurrentUser().isPresent()
                ? JwtService.getCurrentUser().get()
                : "System";
    }
}
