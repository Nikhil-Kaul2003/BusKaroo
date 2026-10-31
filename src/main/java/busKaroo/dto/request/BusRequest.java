package busKaroo.dto.request;

import busKaroo.model.enums.BusStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BusRequest(
        @NotBlank @Size(max = 20) String registrationNumber,
        @NotBlank @Size(max = 100) String model,
        @NotBlank @Size(max = 100) String manufacturer,
        @Min(2000) @Max(2035) int year,
        @Min(1) @Max(100) int totalSeats,
        @NotBlank String seatLayout,
        @NotNull BusStatus status,
        @NotNull Long categoryId) {}
