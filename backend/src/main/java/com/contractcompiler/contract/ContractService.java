package com.contractcompiler.contract;

import com.contractcompiler.analysis.AnalysisPipeline;
import com.contractcompiler.auth.AppUser;
import com.contractcompiler.common.exception.InvalidFileException;
import com.contractcompiler.common.exception.NotFoundException;
import com.contractcompiler.common.config.AppProperties;
import com.contractcompiler.common.util.HashUtils;
import com.contractcompiler.contract.dto.ContractResponse;
import com.contractcompiler.contract.dto.UploadResponse;
import com.contractcompiler.contract.storage.FileStorage;
import com.contractcompiler.finding.FindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for contract and version management.
 * Handles upload, storage, and retrieval of contracts.
 * Analysis is delegated to the analysis pipeline (called after version creation).
 */
@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractVersionRepository versionRepository;
    private final FindingRepository findingRepository;
    private final FileStorage fileStorage;
    private final AppProperties appProperties;
    private final AnalysisPipeline analysisPipeline;

    public ContractService(ContractRepository contractRepository,
                           ContractVersionRepository versionRepository,
                           FindingRepository findingRepository,
                           FileStorage fileStorage,
                           AppProperties appProperties,
                           AnalysisPipeline analysisPipeline) {
        this.contractRepository = contractRepository;
        this.versionRepository = versionRepository;
        this.findingRepository = findingRepository;
        this.fileStorage = fileStorage;
        this.appProperties = appProperties;
        this.analysisPipeline = analysisPipeline;
    }

    /**
     * Creates a new contract and uploads the first version.
     */
    @Transactional
    public UploadResponse createContract(String title, MultipartFile file, AppUser currentUser) {
        validateFile(file);

        Contract contract = new Contract(title, currentUser);
        contractRepository.save(contract);

        ContractVersion version = createVersion(contract, file, currentUser);
        return toUploadResponse(contract, version);
    }

    /**
     * Uploads a new version to an existing contract.
     */
    @Transactional
    public UploadResponse uploadVersion(Long contractId, MultipartFile file, AppUser currentUser) {
        validateFile(file);

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Contract", contractId));

        ContractVersion version = createVersion(contract, file, currentUser);
        return toUploadResponse(contract, version);
    }

    /**
     * Returns all contracts with summary information.
     */
    @Transactional(readOnly = true)
    public List<ContractResponse> listContracts() {
        return contractRepository.findAllWithCreator().stream()
                .map(this::toContractResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single contract with all versions.
     */
    @Transactional(readOnly = true)
    public ContractResponse getContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Contract", contractId));
        return toContractResponse(contract);
    }

    /**
     * Retrieves a version entity by ID, throwing NotFoundException if absent.
     */
    @Transactional(readOnly = true)
    public ContractVersion getVersion(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("ContractVersion", versionId));
    }

    // --- Private helpers ---

    private ContractVersion createVersion(Contract contract, MultipartFile file, AppUser user) {
        String originalName = file.getOriginalFilename();
        FileType fileType = resolveFileType(originalName);

        String storagePath;
        String sha256;

        try (InputStream inputStream = file.getInputStream()) {
            storagePath = fileStorage.store(inputStream, originalName);
        } catch (IOException e) {
            throw new InvalidFileException("Failed to store uploaded file: " + e.getMessage());
        }

        try (InputStream inputStream = file.getInputStream()) {
            sha256 = HashUtils.sha256(inputStream);
        } catch (IOException e) {
            throw new InvalidFileException("Failed to compute file hash: " + e.getMessage());
        }

        int versionNo = contract.nextVersionNo();

        ContractVersion version = new ContractVersion(
                contract, versionNo, originalName, fileType,
                storagePath, sha256, user
        );
        contract.addVersion(version);
        versionRepository.save(version);

        // Trigger async analysis pipeline — runs in background, does not block upload response
        analysisPipeline.analyzeAsync(version.getId());

        return version;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is required.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new InvalidFileException("File name is required.");
        }

        String extension = extractExtension(originalName).toLowerCase();
        List<String> allowedExtensions = appProperties.upload().allowedExtensions();
        if (!allowedExtensions.contains(extension)) {
            throw new InvalidFileException("File type '" + extension + "' is not allowed. Allowed: " + allowedExtensions);
        }

        String mimeType = file.getContentType();
        List<String> allowedMimeTypes = appProperties.upload().allowedMimeTypes();
        if (mimeType != null && !allowedMimeTypes.contains(mimeType)) {
            throw new InvalidFileException("MIME type '" + mimeType + "' is not allowed.");
        }

        long maxBytes = (long) appProperties.upload().maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new InvalidFileException("File exceeds maximum size of " + appProperties.upload().maxFileSizeMb() + " MB.");
        }
    }

    private FileType resolveFileType(String fileName) {
        String ext = extractExtension(fileName).toUpperCase();
        try {
            return FileType.valueOf(ext);
        } catch (IllegalArgumentException e) {
            throw new InvalidFileException("Unsupported file type: " + ext);
        }
    }

    private String extractExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            throw new InvalidFileException("File must have an extension.");
        }
        return fileName.substring(lastDot + 1);
    }

    private ContractResponse toContractResponse(Contract contract) {
        List<ContractResponse.VersionSummary> versionSummaries = contract.getVersions().stream()
                .map(v -> new ContractResponse.VersionSummary(
                        v.getId(), v.getVersionNo(), v.getFileName(),
                        v.getFileType().name(), v.getStatus().name(),
                        findingRepository.countByVersionId(v.getId()),
                        v.getUploadedAt(), v.getAnalyzedAt()
                ))
                .collect(Collectors.toList());

        return new ContractResponse(
                contract.getId(), contract.getTitle(), contract.getStatus().name(),
                contract.getCreatedBy().getFullName(),
                contract.getCreatedAt(), contract.getUpdatedAt(),
                versionSummaries
        );
    }

    private UploadResponse toUploadResponse(Contract contract, ContractVersion version) {
        return new UploadResponse(
                contract.getId(), version.getId(), version.getVersionNo(),
                version.getStatus().name(),
                findingRepository.countByVersionId(version.getId()),
                version.getUploadedAt()
        );
    }
}
