package busKaroo.controller;

import busKaroo.dto.mapper.BusMapper;
import busKaroo.dto.request.BusRequest;
import busKaroo.dto.response.BusResponse;
import busKaroo.model.Bus;
import busKaroo.model.enums.BusStatus;
import busKaroo.service.BusService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/buses")
public class BusController {

    private final BusService busService;
    private final BusMapper busMapper;

    public BusController(BusService busService, BusMapper busMapper) {
        this.busService = busService;
        this.busMapper = busMapper;
    }

    @GetMapping
    public ResponseEntity<List<BusResponse>> getAllBuses() {
        return ResponseEntity.ok(busService.findAll().stream().map(busMapper::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusResponse> getBus(@PathVariable Long id) {
        return ResponseEntity.ok(busMapper.toResponse(busService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BusResponse> createBus(@Valid @RequestBody BusRequest request) {
        Bus created = busService.create(request);
        return ResponseEntity.created(URI.create("/api/buses/" + created.getId()))
                .body(busMapper.toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BusResponse> updateBus(@PathVariable Long id, @Valid @RequestBody BusRequest request) {
        return ResponseEntity.ok(busMapper.toResponse(busService.update(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> updateBusStatus(
            @PathVariable Long id, @RequestBody BusStatus status) {
        busService.updateStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteBus(@PathVariable Long id) {
        busService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
