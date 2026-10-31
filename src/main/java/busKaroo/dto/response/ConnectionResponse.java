package busKaroo.dto.response;

import java.math.BigDecimal;

/** A two-leg itinerary with one transfer between the requested cities. */
public record ConnectionResponse(
        String connectionCity,
        TripResponse firstLeg,
        TripResponse secondLeg,
        BigDecimal totalPrice,
        long layoverMinutes) {}
