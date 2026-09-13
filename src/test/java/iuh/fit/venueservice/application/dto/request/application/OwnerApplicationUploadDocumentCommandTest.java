package iuh.fit.venueservice.application.dto.request.application;

import iuh.fit.venueservice.domain.enums.OwnerApplicationDocumentSlot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnerApplicationUploadDocumentCommandTest {
    private static final long TWO_MB = 2L * 1024 * 1024;

    @Test
    void acceptsExpectedFormatsAtMaximumSize() {
        assertThatCode(() -> new OwnerApplicationUploadDocumentCommand(
                OwnerApplicationDocumentSlot.BUSINESS_LICENSE,
                "license.pdf",
                "application/pdf",
                TWO_MB
        ).validate()).doesNotThrowAnyException();

        assertThatCode(() -> new OwnerApplicationUploadDocumentCommand(
                OwnerApplicationDocumentSlot.VENUE_PHOTO,
                "venue.webp",
                "image/webp",
                TWO_MB
        ).validate()).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnImageInTheBusinessLicenseSlot() {
        OwnerApplicationUploadDocumentCommand command = new OwnerApplicationUploadDocumentCommand(
                OwnerApplicationDocumentSlot.BUSINESS_LICENSE,
                "license.png",
                "image/png",
                1024
        );

        assertThatThrownBy(command::validate)
                .hasMessage("Giấy phép kinh doanh chỉ nhận file PDF.");
    }

    @Test
    void rejectsAFileWhoseExtensionDoesNotMatchItsImageMimeType() {
        OwnerApplicationUploadDocumentCommand command = new OwnerApplicationUploadDocumentCommand(
                OwnerApplicationDocumentSlot.IDENTITY_BACK,
                "back.pdf",
                "image/jpeg",
                1024
        );

        assertThatThrownBy(command::validate)
                .hasMessage("CCCD và ảnh sân chỉ nhận ảnh JPG, PNG hoặc WebP.");
    }

    @Test
    void rejectsFilesLargerThanTwoMegabytes() {
        OwnerApplicationUploadDocumentCommand command = new OwnerApplicationUploadDocumentCommand(
                OwnerApplicationDocumentSlot.IDENTITY_FRONT,
                "front.jpg",
                "image/jpeg",
                TWO_MB + 1
        );

        assertThatThrownBy(command::validate)
                .hasMessage("Mỗi tài liệu phải có dung lượng không quá 2 MB.");
    }
}
