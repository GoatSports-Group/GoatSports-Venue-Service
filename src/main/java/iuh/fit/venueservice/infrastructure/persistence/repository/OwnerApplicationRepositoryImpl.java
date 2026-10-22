package iuh.fit.venueservice.infrastructure.persistence.repository;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.entity.OwnerApplicationDocument;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.AddressEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.mapper.OwnerApplicationDocumentEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.mapper.OwnerApplicationEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaOwnerApplicationRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OwnerApplicationRepositoryImpl implements OwnerApplicationRepository {
    private final JpaOwnerApplicationRepository jpaRepository;
    private final FilterSpecificationConverter filterSpecificationConverter;
    private final FilterParser filterParser;
    private final OwnerApplicationEntityMapper ownerApplicationEntityMapper;
    private final OwnerApplicationDocumentEntityMapper ownerApplicationDocumentEntityMapper;
    private final AddressEntityMapper addressEntityMapper;
    private final JpaVenueRepository jpaVenueRepository;
    private final EntityManager entityManager;

    @Override
    public OwnerApplication create(OwnerApplication ownerApplication) {
        OwnerApplicationEntity entity = ownerApplicationEntityMapper.createEntity(
                ownerApplication,
                addressEntityMapper
        );
        Set<OwnerApplicationDocument> documents =
                ownerApplication.getDocuments() == null ? Set.of() : ownerApplication.getDocuments();
        entity.setOwnerApplicationDocuments(documents.stream()
                .map(document -> ownerApplicationDocumentEntityMapper.createEntity(document, entity))
                .collect(Collectors.toSet()));
        entityManager.persist(entity);
        return ownerApplicationEntityMapper.toDomain(entity);
    }

    @Override
    public OwnerApplication save(OwnerApplication ownerApplication) throws ServiceException {
        OwnerApplicationEntity entity;
        if (ownerApplication.getOwnerApplicationId() != null) {
            entity = jpaRepository.findById(ownerApplication.getOwnerApplicationId())
                    .map(existing -> {
                        ownerApplicationEntityMapper.updateEntity(ownerApplication, existing, addressEntityMapper);
                        return existing;
                    })
                    .orElseThrow(() -> new ServiceException("Đơn đăng ký không tồn tại"));
        } else {
            entity = ownerApplicationEntityMapper.createEntity(ownerApplication, addressEntityMapper);
        }

        if (ownerApplication.getVenueId() != null) {
            entity.setVenue(jpaVenueRepository.getReferenceById(ownerApplication.getVenueId()));
        }

        OwnerApplicationEntity saved = jpaRepository.save(entity);

        return ownerApplicationEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<OwnerApplication> findById(UUID id) {
        return jpaRepository.findDetailedById(id).map(ownerApplicationEntityMapper::toDomain);
    }

    @Override
    public Optional<OwnerApplication> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(ownerApplicationEntityMapper::toDomain);
    }

    @Override
    public PageResult<OwnerApplication> findByUserId(PageFilter filter, UUID userId) {
        int pageNum = Math.max(0, filter.page() - 1);
        int pageSize = filter.size() > 0 ? filter.size() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<OwnerApplicationEntity> spec = (root, query, cb) ->
                cb.equal(root.get("userId"), userId);
        if (filter.filter() != null && !filter.filter().trim().isEmpty()) {
            spec = filterSpecificationConverter.convert(filterParser.parse(filter.filter()));
        }

        Page<OwnerApplicationEntity> entityPage = jpaRepository.findAll(spec, pageable);
        List<OwnerApplication> items = entityPage.getContent().stream()
                .map(ownerApplicationEntityMapper::toDomain)
                .toList();

        return new PageResult<>(
                items,
                entityPage.getTotalElements(),
                filter.page(),
                filter.size(),
                entityPage.getTotalPages()
        );
    }

    @Override
    public boolean existsByUserIdAndStatusIn(UUID userId, List<OwnerApplicationStatus> statuses) {
        return jpaRepository.existsByUserIdAndStatusIn(userId, statuses);
    }

    @Override
    public List<OwnerApplication> findWithTemporaryDocuments(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.ASC, "createdAt"));
        List<UUID> applicationIds = jpaRepository.findIdsWithDocumentFileUrlPrefix("temp/", pageable);
        if (applicationIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findDetailedByIdIn(applicationIds).stream()
                .map(ownerApplicationEntityMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<OwnerApplication> findAll(PageFilter filter) {
        int pageNum = Math.max(0, filter.page() - 1);
        int pageSize = filter.size() > 0 ? filter.size() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<OwnerApplicationEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter.filter() != null && !filter.filter().trim().isEmpty()) {
            spec = filterSpecificationConverter.convert(filterParser.parse(filter.filter()));
        }

        Page<OwnerApplicationEntity> entityPage = jpaRepository.findAll(spec, pageable);
        List<OwnerApplication> items = entityPage.getContent().stream()
                .map(ownerApplicationEntityMapper::toDomain)
                .toList();

        return new PageResult<>(
                items,
                entityPage.getTotalElements(),
                filter.page(),
                filter.size(),
                entityPage.getTotalPages()
        );
    }

    @Override
    public List<String> eraseApplicantData(UUID userId) {
        List<String> fileKeys = new ArrayList<>();
        for (OwnerApplicationEntity application : jpaRepository.findAllByUserId(userId)) {
            application.getOwnerApplicationDocuments().forEach(document -> fileKeys.add(document.getFileUrl()));
            application.getOwnerApplicationDocuments().clear();
            application.setFullName(null);
            application.setPhone(null);
            application.setEmail(null);
            application.setIdentityNumber(null);
        }
        return fileKeys;
    }
}
