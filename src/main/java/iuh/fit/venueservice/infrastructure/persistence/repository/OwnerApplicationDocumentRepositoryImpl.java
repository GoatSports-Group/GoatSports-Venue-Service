package iuh.fit.venueservice.infrastructure.persistence.repository;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationDocumentRepository;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationDocumentEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.OwnerApplicationDocumentMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaOwnerApplicationDocumentRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaOwnerApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OwnerApplicationDocumentRepositoryImpl implements OwnerApplicationDocumentRepository {
    private final JpaOwnerApplicationDocumentRepository jpaOwnerApplicationDocumentRepository;
    private final JpaOwnerApplicationRepository jpaOwnerApplicationRepository;
    private final OwnerApplicationDocumentMapper ownerApplicationDocumentMapper;

    @Override
    public OwnerApplicationDocument save(
            OwnerApplicationDocument document, UUID ownerApplicationId
    ) throws ServiceException
    {
        OwnerApplicationEntity ownerAppEntity = jpaOwnerApplicationRepository
                .findById(ownerApplicationId)
                .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại."));

        OwnerApplicationDocumentEntity entity = ownerApplicationDocumentMapper.createEntity(
                document, ownerAppEntity
        );

        OwnerApplicationDocumentEntity saved = jpaOwnerApplicationDocumentRepository.save(entity);

        return ownerApplicationDocumentMapper.toDomain(saved);
    }
}
