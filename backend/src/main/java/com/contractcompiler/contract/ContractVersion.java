package com.contractcompiler.contract;

import com.contractcompiler.auth.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for the {@code contract_version} table.
 * Represents a specific uploaded version of a contract document.
 */
@Entity
@Table(name = "contract_version",
       uniqueConstraints = @UniqueConstraint(columnNames = {"contract_id", "version_no"}))
public class ContractVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 10)
    private FileType fileType;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "file_sha256", nullable = false, length = 64)
    private String fileSha256;

    @Column(name = "page_count")
    private Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VersionStatus status = VersionStatus.ANALYZING;

    @Column(name = "unparsed_lines", nullable = false)
    private int unparsedLines = 0;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false, updatable = false)
    private AppUser uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt = Instant.now();

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    protected ContractVersion() {}

    public ContractVersion(Contract contract, int versionNo, String fileName,
                           FileType fileType, String storagePath, String fileSha256,
                           AppUser uploadedBy) {
        this.contract = contract;
        this.versionNo = versionNo;
        this.fileName = fileName;
        this.fileType = fileType;
        this.storagePath = storagePath;
        this.fileSha256 = fileSha256;
        this.uploadedBy = uploadedBy;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public Contract getContract() { return contract; }
    public int getVersionNo() { return versionNo; }
    public String getFileName() { return fileName; }
    public FileType getFileType() { return fileType; }
    public String getStoragePath() { return storagePath; }
    public String getFileSha256() { return fileSha256; }
    public Integer getPageCount() { return pageCount; }
    public VersionStatus getStatus() { return status; }
    public int getUnparsedLines() { return unparsedLines; }
    public String getFailureReason() { return failureReason; }
    public AppUser getUploadedBy() { return uploadedBy; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getAnalyzedAt() { return analyzedAt; }

    // --- Setters ---

    void setContract(Contract contract) { this.contract = contract; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public void setUnparsedLines(int unparsedLines) { this.unparsedLines = unparsedLines; }

    public void setStatus(VersionStatus status) { this.status = status; }

    public void markReviewReady(int unparsedLines) {
        this.status = VersionStatus.REVIEW_READY;
        this.unparsedLines = unparsedLines;
        this.analyzedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = VersionStatus.FAILED;
        this.failureReason = reason;
        this.analyzedAt = Instant.now();
    }
}
