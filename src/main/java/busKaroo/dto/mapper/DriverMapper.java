package busKaroo.dto.mapper;

import busKaroo.dto.response.DriverResponse;
import busKaroo.model.Driver;

import org.springframework.stereotype.Component;

@Component
public class DriverMapper {
    public DriverResponse toResponse(Driver driver) {
        return new DriverResponse(
                driver.getUserId(),
                driver.getUser().getFirstName(),
                driver.getUser().getLastName(),
                driver.getUser().getEmail(),
                driver.getLicenseNumber(),
                driver.isAvailable());
    }
}
