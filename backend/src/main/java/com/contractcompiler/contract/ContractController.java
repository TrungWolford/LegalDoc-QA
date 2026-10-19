package com.contractcompiler.contract;

import com.contractcompiler.auth.AppUser;
import com.contractcompiler.contract.dto.ContractResponse;
import com.contractcompiler.contract.dto.UploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for contract and version management.
 * All endpoints require JWT authentication (REVIEWER or ADMIN role).
 */
@RestController
@RequestMapping("/api/contracts")
@Tag(name = "Contracts", description = "Contract upload and management")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new contract with the first version (multipart upload)")
    public ResponseEntity<UploadResponse> createContract(
            @RequestPart("title") @NotBlank String title,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AppUser currentUser) {
        UploadResponse response = contractService.createContract(title, file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all contracts with version summaries")
    public ResponseEntity<List<ContractResponse>> listContracts() {
        return ResponseEntity.ok(contractService.listContracts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contract details with all versions")
    public ResponseEntity<ContractResponse> getContract(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.getContract(id));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new version to an existing contract")
    public ResponseEntity<UploadResponse> uploadVersion(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal AppUser currentUser) {
        UploadResponse response = contractService.uploadVersion(id, file, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
