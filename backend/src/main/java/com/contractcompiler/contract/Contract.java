package com.contractcompiler.contract;

import com.contractcompiler.auth.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code contract} table.
 * Represents a legal contract with one or more uploaded versions.
 */
@Entity
@Table(name = "contract")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContractStatus status = ContractStatus.IN_REVIEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNo ASC")
    private List<ContractVersion> versions = new ArrayList<>();

    protected Contract() {}

    public Contract(String title, AppUser createdBy) {
        this.title = title;
        this.createdBy = createdBy;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public ContractStatus getStatus() { return status; }
    public AppUser getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ContractVersion> getVersions() { return versions; }

    // --- Setters ---

    public void setTitle(String title) { this.title = title; }

    public void setStatus(ContractStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    // --- Helper ---

    public void addVersion(ContractVersion version) {
        versions.add(version);
        version.setContract(this);
    }

    /**
     * Returns the next version number based on existing versions.
     */
    public int nextVersionNo() {
        return versions.stream()
                .mapToInt(ContractVersion::getVersionNo)
                .max()
                .orElse(0) + 1;
    }
}
