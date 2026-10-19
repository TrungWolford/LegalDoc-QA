package com.contractcompiler.analysis.persistence;

import com.contractcompiler.contract.ContractVersion;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for the {@code clause} table.
 * Represents a node in the contract clause tree (Article → Section → Clause),
 * stored as an adjacency list.
 */
@Entity
@Table(name = "clause",
       uniqueConstraints = @UniqueConstraint(columnNames = {"version_id", "seq"}))
public class ClauseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private ContractVersion version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ClauseEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seq ASC")
    private List<ClauseEntity> children = new ArrayList<>();

    /** 1 = Article, 2 = Section, 3 = Clause */
    @Column(nullable = false)
    private short level;

    /** Label such as "2", "2.1", "2.1(a)" */
    @Column(nullable = false, length = 30)
    private String label;

    @Column(length = 255)
    private String title;

    /** Normalized body text of this clause */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** Sequential order within the document */
    @Column(nullable = false)
    private int seq;

    @Column
    private Integer page;

    @Column(name = "start_offset", nullable = false)
    private int startOffset;

    @Column(name = "end_offset", nullable = false)
    private int endOffset;

    protected ClauseEntity() {}

    public ClauseEntity(ContractVersion version, ClauseEntity parent, short level,
                        String label, String title, String body, int seq,
                        Integer page, int startOffset, int endOffset) {
        this.version = version;
        this.parent = parent;
        this.level = level;
        this.label = label;
        this.title = title;
        this.body = body;
        this.seq = seq;
        this.page = page;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public ContractVersion getVersion() { return version; }
    public ClauseEntity getParent() { return parent; }
    public List<ClauseEntity> getChildren() { return children; }
    public short getLevel() { return level; }
    public String getLabel() { return label; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public int getSeq() { return seq; }
    public Integer getPage() { return page; }
    public int getStartOffset() { return startOffset; }
    public int getEndOffset() { return endOffset; }
}
