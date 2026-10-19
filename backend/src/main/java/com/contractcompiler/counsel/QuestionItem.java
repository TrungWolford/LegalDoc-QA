package com.contractcompiler.counsel;

import com.contractcompiler.finding.Finding;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * JPA entity for the {@code question_item} table.
 * Represents a single question within a counsel question package,
 * linked to a specific finding.
 */
@Entity
@Table(name = "question_item",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"package_id", "position"}),
           @UniqueConstraint(columnNames = {"package_id", "finding_id"})
       })
public class QuestionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private QuestionPackage questionPackage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;

    @Column(nullable = false)
    private int position;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /** Suggested answer options (e.g. ["2% đúng", "2.5% đúng", "Khác"]) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> options;

    @Column(name = "answer_choice", length = 255)
    private String answerChoice;

    @Column(name = "answer_note", columnDefinition = "TEXT")
    private String answerNote;

    @Column(name = "answered_at")
    private Instant answeredAt;

    protected QuestionItem() {}

    public QuestionItem(Finding finding, int position, String questionText, List<String> options) {
        this.finding = finding;
        this.position = position;
        this.questionText = questionText;
        this.options = options;
    }

    // --- Getters ---

    public Long getId() { return id; }
    public QuestionPackage getQuestionPackage() { return questionPackage; }
    public Finding getFinding() { return finding; }
    public int getPosition() { return position; }
    public String getQuestionText() { return questionText; }
    public List<String> getOptions() { return options; }
    public String getAnswerChoice() { return answerChoice; }
    public String getAnswerNote() { return answerNote; }
    public Instant getAnsweredAt() { return answeredAt; }

    // --- Setters ---

    void setQuestionPackage(QuestionPackage questionPackage) {
        this.questionPackage = questionPackage;
    }

    /**
     * Records an answer from counsel.
     */
    public void answer(String choice, String note) {
        this.answerChoice = choice;
        this.answerNote = note;
        this.answeredAt = Instant.now();
    }

    public boolean isAnswered() {
        return answeredAt != null;
    }
}
