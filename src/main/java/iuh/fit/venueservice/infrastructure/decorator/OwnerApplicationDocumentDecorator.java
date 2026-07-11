package iuh.fit.venueservice.infrastructure.decorator;

import iuh.fit.venueservice.application.dto.request.file.FileMoveRequest;
import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.application.service.OwnerApplicationDocumentService;
import iuh.fit.venueservice.application.service.impl.OwnerApplicationDocumentServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerApplicationDocumentDecorator implements OwnerApplicationDocumentService {
    private final OwnerApplicationDocumentServiceImpl ownerApplicationDocumentServiceImpl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOwnerApplicationDocument(
            String ownerApplicationId, List<FileMoveRequest> requests
    ) throws ServiceException
    {
        ownerApplicationDocumentServiceImpl.createOwnerApplicationDocument(ownerApplicationId, requests);
    }
}
