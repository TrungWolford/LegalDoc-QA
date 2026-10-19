package com.contractcompiler.counsel;

import com.contractcompiler.auth.AppUser;
import com.contractcompiler.counsel.dto.CreatePackageRequest;
import com.contractcompiler.counsel.dto.PackageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for reviewer-facing package management.
 * All endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/packages")
@Tag(name = "Question Packages", description = "Create and send question packages to counsel")
public class PackageController {

    private final PackageService packageService;

    public PackageController(PackageService packageService) {
        this.packageService = packageService;
    }

    @PostMapping
    @Operation(summary = "Create a new question package from selected findings")
    public ResponseEntity<PackageResponse> createPackage(
            @Valid @RequestBody CreatePackageRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        PackageResponse response = packageService.createPackage(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Send the question package email to counsel")
    public ResponseEntity<Void> sendPackage(@PathVariable Long id) {
        packageService.sendPackage(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Generate a new token and resend the package (invalidates old token)")
    public ResponseEntity<Void> resendPackage(@PathVariable Long id) {
        packageService.sendPackage(id); // Same logic: revokes old token, issues new one
        return ResponseEntity.ok().build();
    }
}
