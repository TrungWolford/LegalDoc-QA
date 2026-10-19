package com.contractcompiler.counsel;

import com.contractcompiler.auth.AppUser;
import com.contractcompiler.contract.Contract;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code question_package} table.
 * Represents a package of questions sent to counsel about specific findings.
 */
@Entity
@Table(name = "question_package")
public class QuestionPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "counsel_name", length = 150)
    private String counselName;

    @Column(name = "counsel_email", nullable = false, length = 255)
    private String counselEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PackageStatus status = PackageStatus.DRAFT;

    /** SHA-256 hash of the counsel token (the real token is only in the email) */
    @Column(name = "token_hash", length = 64, columnDefinition = "CHAR(64)")
    private String tokenHash;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "send_count", nullable = false)
    private int sendCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "questionPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuestionItem> items = new ArrayList<>();

    protected QuestionPackage() {}

    public QuestionPackage(Contract contract, String title, String counselEmail,
                           String counselName, AppUser createdBy) {
        this.contract = contract;
        this.title = title;
        this.counselEmail = counselEmail;
        this.counselName = counselName;
        this.createdBy = createdBy;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public Contract getContract() { return contract; }
    public String getTitle() { return title; }
    public String getCounselName() { return counselName; }
    public String getCounselEmail() { return counselEmail; }
    public PackageStatus getStatus() { return status; }
    public String getTokenHash() { return tokenHash; }
    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public int getSendCount() { return sendCount; }
    public AppUser getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<QuestionItem> getItems() { return items; }

    // --- Setters ---

    public void setStatus(PackageStatus status) { this.status = status; }

    /**
     * Records that the package was sent with a new token.
     */
    public void markSent(String tokenHash, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.tokenExpiresAt = expiresAt;
        this.sentAt = Instant.now();
        this.sendCount++;
        this.status = PackageStatus.SENT;
    }

    public void markCompleted() {
        this.status = PackageStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markExpired() {
        this.status = PackageStatus.EXPIRED;
    }

    // --- Helper ---

    public void addItem(QuestionItem item) {
        items.add(item);
        item.setQuestionPackage(this);
    }

    /**
     * Checks if all items have been answered.
     */
    public boolean isFullyAnswered() {
        return !items.isEmpty() && items.stream().allMatch(QuestionItem::isAnswered);
    }

    /**
     * Checks if any item has been answered.
     */
    public boolean isPartiallyAnswered() {
        return items.stream().anyMatch(QuestionItem::isAnswered);
    }
}
