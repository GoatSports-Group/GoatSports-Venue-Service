package iuh.fit.venueservice.presentation.controller.admin;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.response.application.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.OwnerApplicationMapper;
import iuh.fit.venueservice.application.service.OwnerApplicationService;
import iuh.fit.venueservice.presentation.converter.OwnerApplicationConverter;
import iuh.fit.venueservice.presentation.response.application.OwnerApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/owner-applications")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class OwnerApplicationAdminController {

    private final OwnerApplicationService ownerApplicationService;
    private final OwnerApplicationMapper ownerApplicationMapper;
    private final OwnerApplicationConverter ownerApplicationConverter;

    @GetMapping
    public ResponseEntity<BaseListResponse> getAllOwnerApplications(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable
    ) {
        PageFilter query = new PageFilter(filter, pageable.getPageNumber() + 1, pageable.getPageSize());
        PageResult<OwnerApplicationResponseCommand> resultDto = ownerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.getAllOwnerApplications(query)
        );

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(resultDto.page());
        meta.setPageSize(resultDto.pageSize());
        meta.setPages(resultDto.totalPages());
        meta.setTotal(resultDto.total());

        List<OwnerApplicationResponse> responses = resultDto.items().stream()
                .map(ownerApplicationConverter::toResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(new BaseListResponse(meta, responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OwnerApplicationResponse> getOwnerApplicationDetail(
            @PathVariable String id
    ) throws ServiceException
    {
        OwnerApplicationResponseCommand result = ownerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationService.getOwnerApplicationDetail(id)
        );
        return ResponseEntity.status(HttpStatus.OK).body(ownerApplicationConverter.toResponse(result));
    }

}
