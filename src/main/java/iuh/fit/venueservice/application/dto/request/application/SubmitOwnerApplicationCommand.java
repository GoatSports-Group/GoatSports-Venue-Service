package iuh.fit.venueservice.application.dto.request.application;

import iuh.fit.venueservice.application.exception.ServiceException;
import iuh.fit.venueservice.domain.enums.BusinessType;
import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record SubmitOwnerApplicationCommand(
        UUID ownerApplicationRequestId,
        UUID userId,
        UUID identityVerificationId,
        String fullName,
        String phone,
        String email,
        String identityNumber,
        String businessName,
        BusinessType businessType,
        String taxCode,
        String address,
        String province,
        String district,
        String ward,
        String city,
        Double latitude,
        Double longitude,
        List<OwnerApplicationUploadedDocumentCommand> documents
) {
    private static final Set<OwnerApplicationDocumentSlot> REQUIRED_SLOTS = Set.of(
            OwnerApplicationDocumentSlot.IDENTITY_FRONT,
            OwnerApplicationDocumentSlot.IDENTITY_BACK,
            OwnerApplicationDocumentSlot.BUSINESS_LICENSE,
            OwnerApplicationDocumentSlot.VENUE_PHOTO
    );

    public void validate() throws ServiceException {
        if (ownerApplicationRequestId == null || userId == null || identityVerificationId == null) {
            throw new ServiceException("Không xác định được đơn đăng ký chủ sân.");
        }
        requireText(fullName, "Họ tên");
        requireText(phone, "Số điện thoại");
        requireText(email, "Email");
        requireText(identityNumber, "Số CCCD/Hộ chiếu");
        requireText(businessName, "Tên doanh nghiệp/Hộ kinh doanh");
        if (businessType == null) {
            throw new ServiceException("Loại hình kinh doanh không được để trống.");
        }
        requireText(taxCode, "Mã số thuế");
        requireText(address, "Địa chỉ chi tiết");
        requireText(province, "Tỉnh/Thành phố");
        requireText(ward, "Phường/Xã");
        requireText(city, "Thành phố");
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new ServiceException("Vĩ độ không hợp lệ.");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new ServiceException("Kinh độ không hợp lệ.");
        }
        if (documents == null || documents.size() != REQUIRED_SLOTS.size()) {
            throw new ServiceException("Hồ sơ phải có đủ mặt trước, mặt sau giấy tờ, giấy phép và ảnh sân.");
        }
        for (OwnerApplicationUploadedDocumentCommand document : documents) {
            if (document == null) {
                throw new ServiceException("Thông tin tài liệu không hợp lệ.");
            }
            document.validate(userId);
        }
        Set<OwnerApplicationDocumentSlot> actualSlots = documents.stream()
                .map(OwnerApplicationUploadedDocumentCommand::slot)
                .collect(Collectors.toSet());
        if (!actualSlots.equals(REQUIRED_SLOTS) || actualSlots.size() != documents.size()) {
            throw new ServiceException("Loại tài liệu bị thiếu hoặc trùng lặp.");
        }
        if (documents.stream().map(OwnerApplicationUploadedDocumentCommand::objectKey).distinct().count()
                != documents.size()) {
            throw new ServiceException("Danh sách tài liệu có object key bị trùng.");
        }
    }

    public SubmitOwnerApplicationCommand withVerifiedIdentity(String verifiedFullName, String verifiedIdentityNumber) {
        return new SubmitOwnerApplicationCommand(
                ownerApplicationRequestId, userId, identityVerificationId,
                verifiedFullName, phone, email, verifiedIdentityNumber,
                businessName, businessType, taxCode, address, province, district, ward, city,
                latitude, longitude, documents
        );
    }

    private void requireText(String value, String fieldName) throws ServiceException {
        if (value == null || value.isBlank()) {
            throw new ServiceException(fieldName + " không được để trống.");
        }
    }
}
