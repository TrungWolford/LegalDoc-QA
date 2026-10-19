package com.contractcompiler.finding;

import com.contractcompiler.common.exception.NotFoundException;
import com.contractcompiler.contract.ContractVersion;
import com.contractcompiler.contract.ContractVersionRepository;
import com.contractcompiler.finding.dto.FindingResponse;
import com.contractcompiler.finding.dto.VersionCompareResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for comparing findings between two versions of a contract.
 * Uses the {@code fingerprint} field to identify whether findings are
 * still open, resolved, or newly introduced.
 *
 * <p>Per doc 03 §5.2:
 * <ul>
 *   <li>Same fingerprint in both → still open (carried over)</li>
 *   <li>Only in base version → resolved</li>
 *   <li>Only in compare version → new</li>
 * </ul>
 */
@Service
public class VersionCompareService {

    private final ContractVersionRepository versionRepository;
    private final FindingRepository findingRepository;
    private final FindingWorkflowService workflowService;

    public VersionCompareService(ContractVersionRepository versionRepository,
                                 FindingRepository findingRepository,
                                 FindingWorkflowService workflowService) {
        this.versionRepository = versionRepository;
        this.findingRepository = findingRepository;
        this.workflowService = workflowService;
    }

    /**
     * Compares findings between two versions of the same contract.
     *
     * @param baseVersionId    the older version
     * @param compareVersionId the newer version
     */
    @Transactional(readOnly = true)
    public VersionCompareResponse compare(Long baseVersionId, Long compareVersionId) {
        ContractVersion baseVersion = versionRepository.findById(baseVersionId)
                .orElseThrow(() -> new NotFoundException("ContractVersion", baseVersionId));
        ContractVersion compareVersion = versionRepository.findById(compareVersionId)
                .orElseThrow(() -> new NotFoundException("ContractVersion", compareVersionId));

        if (!baseVersion.getContract().getId().equals(compareVersion.getContract().getId())) {
            throw new IllegalArgumentException("Both versions must belong to the same contract.");
        }

        Long contractId = baseVersion.getContract().getId();
        List<Finding> baseFindings = findingRepository.findByContractAndVersion(contractId, baseVersionId);
        List<Finding> compareFindings = findingRepository.findByContractAndVersion(contractId, compareVersionId);

        Set<String> baseFingerprints = baseFindings.stream()
                .map(Finding::getFingerprint)
                .collect(Collectors.toSet());
        Set<String> compareFingerprints = compareFindings.stream()
                .map(Finding::getFingerprint)
                .collect(Collectors.toSet());

        // Still open: fingerprint exists in both versions
        List<FindingResponse> stillOpen = compareFindings.stream()
                .filter(f -> baseFingerprints.contains(f.getFingerprint()))
                .map(workflowService::toResponse)
                .collect(Collectors.toList());

        // Resolved: fingerprint only in base (gone in new version)
        List<FindingResponse> resolved = baseFindings.stream()
                .filter(f -> !compareFingerprints.contains(f.getFingerprint()))
                .map(workflowService::toResponse)
                .collect(Collectors.toList());

        // New: fingerprint only in compare (didn't exist before)
        List<FindingResponse> newFindings = compareFindings.stream()
                .filter(f -> !baseFingerprints.contains(f.getFingerprint()))
                .map(workflowService::toResponse)
                .collect(Collectors.toList());

        return new VersionCompareResponse(
                baseVersionId, compareVersionId,
                stillOpen, resolved, newFindings
        );
    }
}
