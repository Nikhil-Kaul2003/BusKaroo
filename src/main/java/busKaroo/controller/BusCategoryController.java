package busKaroo.controller;

import busKaroo.dto.request.BusCategoryRequest;
import busKaroo.dto.response.BusCategoryResponse;
import busKaroo.model.BusCategory;
import busKaroo.service.BusCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/bus-categories")
public class BusCategoryController {
    private final BusCategoryService service;

    public BusCategoryController(BusCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<BusCategoryResponse>> getAll() {
        return ResponseEntity.ok(service.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusCategoryResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(service.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BusCategoryResponse> create(@Valid @RequestBody BusCategoryRequest request) {
        BusCategory category = service.create(request);
        return ResponseEntity.created(URI.create("/api/bus-categories/" + category.getId()))
                .body(toResponse(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BusCategoryResponse> update(
            @PathVariable Long id, @Valid @RequestBody BusCategoryRequest request) {
        return ResponseEntity.ok(toResponse(service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private BusCategoryResponse toResponse(BusCategory category) {
        return new BusCategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
