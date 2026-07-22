package iuh.fit.venueservice.application.mapper;

import iuh.fit.venueservice.application.dto.response.application.OwnerApplicationResponseCommand;
import iuh.fit.venueservice.application.dto.response.base.PageResult;
import iuh.fit.venueservice.domain.entity.OwnerApplication;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OwnerApplicationMapper {

    OwnerApplicationResponseCommand toOwnerApplicationResponseCommand(OwnerApplication ownerApplication);

    List<OwnerApplicationResponseCommand> toOwnerApplicationResponseCommandList(List<OwnerApplication> ownerApplications);

    default PageResult<OwnerApplicationResponseCommand> toOwnerApplicationResponseCommand(
            PageResult<OwnerApplication> ownerApplications
    ) {
        if (ownerApplications == null) return null;
        return new PageResult<>(
                toOwnerApplicationResponseCommandList(ownerApplications.items()),
                ownerApplications.total(),
                ownerApplications.page(),
                ownerApplications.pageSize(),
                ownerApplications.totalPages()
        );
    }
}
