package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.domain.enums.ExternalVenueSuggestionStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.ExternalVenueSuggestionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaExternalVenueSuggestionRepository extends JpaRepository<ExternalVenueSuggestionEntity, UUID> {

    Page<ExternalVenueSuggestionEntity> findByStatusOrderByCreatedAtAsc(ExternalVenueSuggestionStatus status,
                                                                       Pageable pageable);

    long countBySuggestedByAndStatus(UUID suggestedBy, ExternalVenueSuggestionStatus status);

    long countByStatus(ExternalVenueSuggestionStatus status);

    @Modifying
    @Query("delete from ExternalVenueSuggestionEntity s where s.suggestedBy = :suggestedBy")
    int deleteBySuggestedBy(@Param("suggestedBy") UUID suggestedBy);
}
