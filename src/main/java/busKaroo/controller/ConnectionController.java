package busKaroo.controller;

import busKaroo.dto.response.ConnectionResponse;
import busKaroo.service.TripService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Public search endpoint for itineraries that need one transfer. */
@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    private final TripService tripService;

    public ConnectionController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ConnectionResponse>> searchConnections(
            @RequestParam String fromCity,
            @RequestParam String toCity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") Integer passengers,
            @RequestParam(defaultValue = "any") String timeWindow) {
        return ResponseEntity.ok(tripService.searchConnections(
                fromCity, toCity, date, passengers, timeWindow));
    }
}
