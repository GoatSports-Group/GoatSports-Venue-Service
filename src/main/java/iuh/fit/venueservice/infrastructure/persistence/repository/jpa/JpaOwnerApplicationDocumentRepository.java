package iuh.fit.venueservice.infrastructure.persistence.repository.jpa;

import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaOwnerApplicationDocumentRepository
        extends JpaRepository<OwnerApplicationDocumentEntity, UUID>, JpaSpecificationExecutor<OwnerApplicationDocumentEntity>
{}
