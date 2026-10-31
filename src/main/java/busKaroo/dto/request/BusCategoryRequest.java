package busKaroo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BusCategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 250) String description) {}
