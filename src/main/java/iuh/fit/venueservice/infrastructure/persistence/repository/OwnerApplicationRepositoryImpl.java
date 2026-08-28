package iuh.fit.venueservice.infrastructure.persistence.repository;

import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.port.persistence.OwnerApplicationRepository;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import iuh.fit.venueservice.domain.enums.OwnerApplicationStatus;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;
import iuh.fit.venueservice.infrastructure.persistence.mapper.AddressEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.mapper.OwnerApplicationEntityMapper;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaOwnerApplicationRepository;
import iuh.fit.venueservice.infrastructure.persistence.repository.jpa.JpaVenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OwnerApplicationRepositoryImpl implements OwnerApplicationRepository {
    private final JpaOwnerApplicationRepository jpaRepository;
    private final FilterSpecificationConverter filterSpecificationConverter;
    private final FilterParser filterParser;
    private final OwnerApplicationEntityMapper ownerApplicationEntityMapper;
    private final AddressEntityMapper addressEntityMapper;
    private final JpaVenueRepository jpaVenueRepository;

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
        return jpaRepository.findById(id).map(ownerApplicationEntityMapper::toDomain);
    }

    @Override
    public Optional<OwnerApplication> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(ownerApplicationEntityMapper::toDomain);
    }

    @Override
    public PageResult<OwnerApplication> findByUserId(PageFilter filter, UUID userId) {
        int pageNum = Math.max(0, filter.page() - 1);
        int pageSize = filter.size() > 0 ? filter.size() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

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
}
