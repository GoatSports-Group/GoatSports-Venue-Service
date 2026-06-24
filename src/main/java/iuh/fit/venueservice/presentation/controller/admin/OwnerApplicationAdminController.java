package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.application.ApproveOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.request.application.RejectOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.application.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.OwnerApplicationMapper;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.presentation.converter.OwnerApplicationConverter;
import iuh.fit.venueservice.presentation.request.application.RejectOwnerApplicationRequest;
import iuh.fit.venueservice.presentation.response.application.OwnerApplicationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/owner-applications")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class OwnerApplicationAdminController {

    private final OwnerApplicationService ownerApplicationService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllOwnerApplications(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable
    ) {
        PageFilter query = new PageFilter(filter, pageable.getPageNumber() + 1, pageable.getPageSize());
        PageResult<OwnerApplicationResponseCommand> resultDto = OwnerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.getAllOwnerApplications(query)
        );

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(resultDto.page());
        meta.setPageSize(resultDto.pageSize());
        meta.setPages(resultDto.totalPages());
        meta.setTotal(resultDto.total());

        List<OwnerApplicationResponse> responses = resultDto.items().stream()
                .map(OwnerApplicationConverter::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(new BaseListResponse(meta, responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OwnerApplicationResponse> getOwnerApplicationDetail(
            @PathVariable String id
    ) throws ServiceException
    {
        OwnerApplicationResponseCommand result = OwnerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.getOwnerApplicationDetail(id)
        );
        return ResponseEntity.status(HttpStatus.OK).body(OwnerApplicationConverter.toResponse(result));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<OwnerApplicationResponse> approveOwnerApplication(
            @PathVariable String id
    ) throws ServiceException
    {
        String reviewerIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));

        UUID reviewerId = BaseUtil.uuid(reviewerIdStr);
        UUID applicationId = BaseUtil.uuid(id);

        ApproveOwnerApplicationCommand command = new ApproveOwnerApplicationCommand(applicationId, reviewerId);
        OwnerApplicationResponseCommand result = OwnerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.approveOwnerApplication(command)
        );

        return ResponseEntity.status(HttpStatus.OK).body(OwnerApplicationConverter.toResponse(result));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OwnerApplicationResponse> rejectApplication(
            @PathVariable String id,
            @Valid @RequestBody RejectOwnerApplicationRequest request
    ) throws ServiceException
    {
        String reviewerIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));

        UUID reviewerId = BaseUtil.uuid(reviewerIdStr);
        UUID applicationId = BaseUtil.uuid(id);

        RejectOwnerApplicationCommand command = new RejectOwnerApplicationCommand(
                applicationId,
                reviewerId,
                request.getRejectReason()
        );
        OwnerApplicationResponseCommand result = OwnerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.rejectOwnerApplication(command)
        );

        return ResponseEntity.status(HttpStatus.OK).body(OwnerApplicationConverter.toResponse(result));
    }
}
