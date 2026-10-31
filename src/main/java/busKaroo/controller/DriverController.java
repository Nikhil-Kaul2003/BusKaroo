package busKaroo.controller;

import busKaroo.dto.mapper.DriverMapper;
import busKaroo.dto.mapper.TripMapper;
import busKaroo.dto.response.DriverResponse;
import busKaroo.dto.response.TripResponse;
import busKaroo.exception.ForbiddenException;
import busKaroo.model.Driver;
import busKaroo.security.SecurityUtils;
import busKaroo.service.DriverService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;
    private final SecurityUtils securityUtils;
    private final DriverMapper driverMapper;
    private final TripMapper tripMapper;

    public DriverController(
            DriverService driverService,
            SecurityUtils securityUtils,
            DriverMapper driverMapper,
            TripMapper tripMapper) {
        this.driverService = driverService;
        this.securityUtils = securityUtils;
        this.driverMapper = driverMapper;
        this.tripMapper = tripMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DriverResponse>> getAllDrivers() {
        return ResponseEntity.ok(
                driverService.findAll().stream().map(driverMapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DRIVER')")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable Long id) {
        // Driver can see only themselves? Or any driver?
        // Let's assume Drivers can see other drivers basic info is okay, or restrict.
        // Spec says GET /api/drivers/{id} without restriction note, but broadly usually secured.
        // Let's restrict to ADMIN or SELF.
        Long currentUserId = securityUtils.getCurrentUserId();
        if (!securityUtils.hasRole("ROLE_ADMIN") && !id.equals(currentUserId)) {
            throw new ForbiddenException("Access denied");
        }
        return ResponseEntity.ok(driverMapper.toResponse(driverService.findById(id)));
    }

    @GetMapping("/me/trips")
    @PreAuthorize("hasAuthority('ROLE_DRIVER')")
    public ResponseEntity<List<TripResponse>> getMyTrips() {
        Long currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                driverService.getTrips(currentUserId).stream()
                        .map(tripMapper::toResponse)
                        .toList());
    }

    @GetMapping("/{id}/trips")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<TripResponse>> getDriverTrips(@PathVariable Long id) {
        return ResponseEntity.ok(
                driverService.getTrips(id).stream().map(tripMapper::toResponse).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DriverResponse> createDriver(@RequestBody Driver driver) {
        Driver created = driverService.create(driver);
        return ResponseEntity.created(URI.create("/api/drivers/" + created.getUserId()))
                .body(driverMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id, @RequestBody Driver driver) {
        return ResponseEntity.ok(driverMapper.toResponse(driverService.update(id, driver)));
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_DRIVER')")
    public ResponseEntity<Void> updateAvailability(
            @PathVariable Long id, @RequestBody boolean isAvailable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        if (!securityUtils.hasRole("ROLE_ADMIN") && !id.equals(currentUserId)) {
            throw new ForbiddenException("Access denied");
        }
        driverService.updateAvailability(id, isAvailable);
        return ResponseEntity.noContent().build();
    }
}
