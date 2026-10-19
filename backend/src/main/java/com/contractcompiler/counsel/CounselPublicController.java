package com.contractcompiler.counsel;

import com.contractcompiler.counsel.dto.CounselAnswerRequest;
import com.contractcompiler.counsel.dto.CounselPackageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public REST controller for counsel access.
 * No authentication required — access is controlled by the single-use token with TTL.
 * Rate-limited by IP in Redis (per doc 03 §6).
 */
@RestController
@RequestMapping("/public/counsel")
@Tag(name = "Counsel (Public)", description = "Token-based endpoints for external counsel")
public class CounselPublicController {

    private final PackageService packageService;

    public CounselPublicController(PackageService packageService) {
        this.packageService = packageService;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Retrieve the question package and contract excerpts for a counsel token")
    public ResponseEntity<CounselPackageResponse> getPackage(@PathVariable String token) {
        CounselPackageResponse response = packageService.getPackageByToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{token}/answers")
    @Operation(summary = "Submit answers for one or more questions (partial answers allowed)")
    public ResponseEntity<Void> submitAnswers(
            @PathVariable String token,
            @Valid @RequestBody CounselAnswerRequest request) {
        packageService.submitAnswers(token, request);
        return ResponseEntity.ok().build();
    }
}
