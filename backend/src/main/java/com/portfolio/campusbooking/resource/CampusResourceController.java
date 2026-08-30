package com.portfolio.campusbooking.resource;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
public class CampusResourceController {

    private final CampusResourceRepository repository;

    public CampusResourceController(CampusResourceRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    List<CampusResourceResponse> listResources() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
                .map(CampusResourceResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    CampusResourceResponse createResource(@Valid @RequestBody CreateCampusResourceRequest request) {
        var resource = repository.save(new CampusResource(
                request.name(), request.resourceType(), request.location(), request.capacity()));
        return CampusResourceResponse.from(resource);
    }

    public record CreateCampusResourceRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull ResourceType resourceType,
            @NotBlank @Size(max = 160) String location,
            @Min(1) int capacity) {
    }

    public record CampusResourceResponse(
            Long id,
            String name,
            ResourceType resourceType,
            String location,
            int capacity,
            boolean active) {

        static CampusResourceResponse from(CampusResource resource) {
            return new CampusResourceResponse(
                    resource.getId(),
                    resource.getName(),
                    resource.getResourceType(),
                    resource.getLocation(),
                    resource.getCapacity(),
                    resource.isActive());
        }
    }
}
