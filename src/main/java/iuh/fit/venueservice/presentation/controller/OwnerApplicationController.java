package iuh.fit.venueservice.presentation.controller;

import iuh.fit.venueservice.application.dto.PageFilter;
import iuh.fit.venueservice.application.dto.request.venue.SubmitOwnerApplicationCommand;
import iuh.fit.venueservice.application.dto.response.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.application.dto.response.base.BaseListResponse;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.mapper.OwnerApplicationMapper;
import iuh.fit.venueservice.application.port.security.CurrentUserProvider;
import iuh.fit.venueservice.application.usecase.OwnerApplicationUseCase;
import iuh.fit.venueservice.presentation.converter.OwnerApplicationConverter;
import iuh.fit.venueservice.presentation.request.venue.SubmitOwnerApplicationRequest;
import iuh.fit.venueservice.presentation.response.venue.OwnerApplicationResponse;
import jakarta.validation.Valid;
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

    private final OwnerApplicationUseCase ownerApplicationUseCase;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<OwnerApplicationResponse> submitApplication(
            @Valid @ModelAttribute SubmitOwnerApplicationRequest request
    ) throws ServiceException
    {
        String userIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));
        UUID userId = UUID.fromString(userIdStr);

        SubmitOwnerApplicationCommand command = new SubmitOwnerApplicationCommand(
                userId,
                request.getFullName(),
                request.getPhone(),
                request.getEmail(),
                request.getIdentityNumber(),
                request.getBusinessName(),
                request.getBusinessType(),
                request.getTaxCode(),
                request.getAddress(),
                request.getProvince(),
                request.getDistrict(),
                request.getWard(),
                request.getCity(),
                request.getDocuments().stream()
                        .map(doc -> {
                            try {
                                return new SubmitOwnerApplicationCommand.DocumentInfo(
                                        doc.getDocumentType(),
                                        doc.getFileUrl().getBytes(),
                                        doc.getFileUrl().getOriginalFilename(),
                                        doc.getFileUrl().getContentType()
                                );
                            } catch (java.io.IOException e) {
                                try {
                                    throw new ServiceException("Không thể đọc file đính kèm: " + doc.getDocumentType().getValue());
                                } catch (ServiceException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                        })
                        .collect(Collectors.toList())
        );

        OwnerApplicationResponseCommand result = OwnerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationUseCase.submitOwnerApplication(command)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(OwnerApplicationConverter.toResponse(result));
    }

    @GetMapping("/me")
    public ResponseEntity<BaseListResponse> getMyOwnerApplications(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable
    ) throws ServiceException
    {
        PageFilter query = new PageFilter(filter, pageable.getPageNumber() + 1, pageable.getPageSize());

        String userIdStr = currentUserProvider.getCurrentUserId()
                .orElseThrow(() -> new ServiceException("Bạn phải đăng nhập để thực hiện chức năng này."));
        UUID userId = UUID.fromString(userIdStr);

        PageResult<OwnerApplicationResponseCommand> result = OwnerApplicationMapper.toOwnerApplicationResponseCommand(
                ownerApplicationUseCase.getMyOwnerApplications(query, userId)
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
