package iuh.fit.venueservice.infrastructure.persistence.mapper;

import iuh.fit.venueservice.domain.entity.Address;
import iuh.fit.venueservice.infrastructure.persistence.entity.AddressEntity;
import iuh.fit.venueservice.infrastructure.persistence.entity.OwnerApplicationEntity;

public class AddressMapper {

    public static AddressEntity toEntity(Address domain, OwnerApplicationEntity parent) {
        if (domain == null) return null;

        return AddressEntity.builder()
            .addressId(domain.getAddressId())
            .address(domain.getAddress())
            .ward(domain.getWard())
            .district(domain.getDistrict())
            .city(domain.getCity())
            .province(domain.getProvince())
            .ownerApplication(parent)
            .build();
    }

    public static Address toDomain(AddressEntity entity) {
        if (entity == null) return null;

        return Address.builder()
                .addressId(entity.getAddressId())
                .address(entity.getAddress())
                .ward(entity.getWard())
                .district(entity.getDistrict())
                .city(entity.getCity())
                .province(entity.getProvince())
                .build();
    }
}
