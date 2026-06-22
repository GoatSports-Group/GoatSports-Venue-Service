package iuh.fit.venueservice.application.dto.response.base;

import java.util.List;

public record PageResult<T>(
    List<T> items,
    long total,
    int page,
    int pageSize,
    int totalPages
) {}
