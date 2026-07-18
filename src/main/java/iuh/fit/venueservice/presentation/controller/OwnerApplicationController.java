package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.response.application.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.OwnerApplicationMapper;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.application.util.BaseUtil;
import iuh.fit.venueservice.presentation.converter.OwnerApplicationConverter;
import iuh.fit.venueservice.presentation.response.application.OwnerApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/owner-applications")
@RequiredArgsConstructor
public class OwnerApplicationController {

    private final OwnerApplicationService ownerApplicationService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/me")
    public ResponseEntity<BaseListResponse> getMyOwnerApplications(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable
    ) throws ServiceException
    {
        PageFilter query = new PageFilter(filter, pageable.getPageNumber() + 1, pageable.getPageSize());

        String userIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));
        UUID userId = BaseUtil.uuid(userIdStr);

        PageResult<OwnerApplicationResponseCommand> result = OwnerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.getMyOwnerApplications(query, userId)
        );

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(result.page());
        meta.setPageSize(result.pageSize());
        meta.setPages(result.totalPages());
        meta.setTotal(result.total());

        List<OwnerApplicationResponse> responses = result.items().stream()
                .map(OwnerApplicationConverter::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(new BaseListResponse(meta, responses));
    }
}
