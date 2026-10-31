package busKaroo.dto.response;

import busKaroo.model.enums.BusStatus;

public record BusResponse(
        Long id,
        String registrationNumber,
        String model,
        String manufacturer,
        short year,
        int totalSeats,
        BusStatus status,
        Long categoryId,
        String categoryName) {}
