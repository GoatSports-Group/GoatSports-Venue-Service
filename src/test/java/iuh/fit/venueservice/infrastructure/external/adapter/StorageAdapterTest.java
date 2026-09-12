package iuh.fit.venueservice.infrastructure.external.adapter;

import iuh.fit.venueservice.infrastructure.external.client.StorageClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageAdapterTest {

    private final StorageClient storageClient = mock(StorageClient.class);
    private final StorageAdapter storageAdapter = new StorageAdapter(storageClient);

    @Test
    void getFileUrlReturnsPlainTextResponseFromStorageService() throws Exception {
        String key = "temp/identities/user/front.jpg";
        String fileUrl = "https://storage.example.com/temp/identities/user/front.jpg";
        when(storageClient.getFileUrl(key)).thenReturn(fileUrl);

        assertThat(storageAdapter.getFileUrl(key)).isEqualTo(fileUrl);
    }

    @Test
    void getFileUrlRejectsBlankStorageResponse() {
        String key = "temp/identities/user/front.jpg";
        when(storageClient.getFileUrl(key)).thenReturn(" ");

        assertThatThrownBy(() -> storageAdapter.getFileUrl(key))
                .hasMessage("Không thể lấy đường dẫn đọc tài liệu tạm.");
    }
}
