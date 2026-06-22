package iuh.fit.venueservice.infrastructure.config;

import iuh.fit.venueservice.application.port.StoragePort;
import iuh.fit.venueservice.application.usecase.StorageUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public StorageUseCase storageUseCase(StoragePort storagePort) {
        return new StorageUseCase(storagePort);
    }

}
