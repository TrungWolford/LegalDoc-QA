package com.contractcompiler.contract;

import com.contractcompiler.analysis.persistence.ClauseEntity;
import com.contractcompiler.analysis.persistence.ClauseRepository;
import com.contractcompiler.contract.dto.VersionTextResponse;
import com.contractcompiler.contract.dto.VersionTextResponse.ClauseNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for serving the parsed contract text structure.
 * Used by the frontend split-screen document viewer.
 */
@RestController
@RequestMapping("/api/versions")
@Tag(name = "Versions", description = "Contract version text and structure")
public class VersionController {

    private final ContractService contractService;
    private final ClauseRepository clauseRepository;

    public VersionController(ContractService contractService,
                             ClauseRepository clauseRepository) {
        this.contractService = contractService;
        this.clauseRepository = clauseRepository;
    }

    @GetMapping("/{vid}/text")
    @Operation(summary = "Get parsed clause tree for document viewer")
    public ResponseEntity<VersionTextResponse> getVersionText(@PathVariable Long vid) {
        ContractVersion version = contractService.getVersion(vid);

        List<ClauseEntity> roots = clauseRepository.findRootsByVersionId(vid);

        List<ClauseNode> clauseNodes = roots.stream()
                .map(this::toClauseNode)
                .collect(Collectors.toList());

        VersionTextResponse response = new VersionTextResponse(
                version.getId(),
                version.getVersionNo(),
                version.getFileName(),
                version.getPageCount() != null ? version.getPageCount() : 0,
                version.getUnparsedLines(),
                clauseNodes
        );

        return ResponseEntity.ok(response);
    }

    private ClauseNode toClauseNode(ClauseEntity clause) {
        List<ClauseNode> children = clause.getChildren().stream()
                .map(this::toClauseNode)
                .collect(Collectors.toList());

        return new ClauseNode(
                clause.getId(),
                clause.getLevel(),
                clause.getLabel(),
                clause.getTitle(),
                clause.getBody(),
                clause.getPage() != null ? clause.getPage() : 0,
                clause.getStartOffset(),
                clause.getEndOffset(),
                children
        );
    }
}
