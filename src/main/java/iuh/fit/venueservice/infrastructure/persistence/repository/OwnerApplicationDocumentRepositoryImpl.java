package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.port.persistence.OwnerApplicationDocumentRepository;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationDocumentEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.OwnerApplicationDocumentMapper;
import iuh.fit.venueservice.infrastructure.persistence.mapper.OwnerApplicationMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaOwnerApplicationDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnerApplicationDocumentRepositoryImpl implements OwnerApplicationDocumentRepository {
    private final JpaOwnerApplicationDocumentRepository jpaOwnerApplicationDocumentRepository;

    @Override
    public OwnerApplicationDocument save(OwnerApplicationDocument document, OwnerApplication ownerApplication) {
        OwnerApplicationDocumentEntity entity = OwnerApplicationDocumentMapper.toEntity(
                document, OwnerApplicationMapper.toEntity(ownerApplication)
        );

        OwnerApplicationDocumentEntity saved = jpaOwnerApplicationDocumentRepository.save(entity);

        return OwnerApplicationDocumentMapper.toDomain(saved);
    }
}
