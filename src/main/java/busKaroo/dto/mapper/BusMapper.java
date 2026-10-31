package busKaroo.dto.mapper;

import busKaroo.dto.response.BusResponse;
import busKaroo.model.Bus;

import org.springframework.stereotype.Component;

@Component
public class BusMapper {
    public BusResponse toResponse(Bus bus) {
        return new BusResponse(
                bus.getId(),
                bus.getRegistrationNumber(),
                bus.getModel(),
                bus.getManufacturer(),
                bus.getYear(),
                bus.getTotalSeats(),
                bus.getStatus(),
                bus.getCategory() == null ? null : bus.getCategory().getId(),
                bus.getCategory() == null ? null : bus.getCategory().getName());
    }
}
