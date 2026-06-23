package iuh.fit.venueservice.application.dto;

public record PageFilter(
    String filter,
    int page,
    int size
) {}
