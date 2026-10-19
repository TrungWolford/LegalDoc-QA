package com.contractcompiler.finding;

import com.contractcompiler.auth.AppUser;
import com.contractcompiler.finding.dto.FindingEventResponse;
import com.contractcompiler.finding.dto.FindingResponse;
import com.contractcompiler.finding.dto.StatusChangeRequest;
import com.contractcompiler.finding.dto.VersionCompareResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for finding management.
 * Provides endpoints for listing findings, changing status, viewing audit trail,
 * and comparing findings between versions.
 */
@RestController
@Tag(name = "Findings", description = "Finding review and workflow management")
public class FindingController {

    private final FindingWorkflowService workflowService;
    private final VersionCompareService compareService;

    public FindingController(FindingWorkflowService workflowService,
                             VersionCompareService compareService) {
        this.workflowService = workflowService;
        this.compareService = compareService;
    }

    @GetMapping("/api/versions/{vid}/findings")
    @Operation(summary = "List findings for a version with optional filters")
    public ResponseEntity<List<FindingResponse>> listFindings(
            @PathVariable Long vid,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ruleId) {
        return ResponseEntity.ok(workflowService.listFindings(vid, severity, status, ruleId));
    }

    @PatchMapping("/api/findings/{id}/status")
    @Operation(summary = "Change finding status (Confirm, Dismiss, Resolve)")
    public ResponseEntity<FindingResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(workflowService.changeStatus(id, request, currentUser));
    }

    @GetMapping("/api/findings/{id}/events")
    @Operation(summary = "Get audit trail for a finding")
    public ResponseEntity<List<FindingEventResponse>> getEvents(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getEvents(id));
    }

    @GetMapping("/api/versions/{vid}/compare/{otherVid}")
    @Operation(summary = "Compare findings between two versions")
    public ResponseEntity<VersionCompareResponse> compareVersions(
            @PathVariable Long vid,
            @PathVariable Long otherVid) {
        return ResponseEntity.ok(compareService.compare(vid, otherVid));
    }
}
