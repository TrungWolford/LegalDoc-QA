package com.contractcompiler.finding;

import com.contractcompiler.auth.AppUser;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity for the {@code finding_event} table.
 * Immutable audit trail record: INSERT only, no UPDATE or DELETE.
 */
@Entity
@Table(name = "finding_event")
public class FindingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finding_id", nullable = false)
    private Finding finding;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private FindingStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private FindingStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10)
    private ActorType actorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected FindingEvent() {}

    /**
     * Creates a status-change event.
     */
    public static FindingEvent statusChanged(Finding finding, FindingStatus from,
                                             FindingStatus to, ActorType actorType,
                                             AppUser actor, String note) {
        FindingEvent event = new FindingEvent();
        event.finding = finding;
        event.action = EventAction.STATUS_CHANGED;
        event.fromStatus = from;
        event.toStatus = to;
        event.actorType = actorType;
        event.actor = actor;
        event.note = note;
        return event;
    }

    /**
     * Creates a CREATED event when a finding is first detected.
     */
    public static FindingEvent created(Finding finding) {
        FindingEvent event = new FindingEvent();
        event.finding = finding;
        event.action = EventAction.CREATED;
        event.toStatus = FindingStatus.DETECTED;
        event.actorType = ActorType.SYSTEM;
        return event;
    }

    /**
     * Creates a generic event with a custom action.
     */
    public static FindingEvent of(Finding finding, EventAction action,
                                  ActorType actorType, AppUser actor, String note) {
        FindingEvent event = new FindingEvent();
        event.finding = finding;
        event.action = action;
        event.actorType = actorType;
        event.actor = actor;
        event.note = note;
        return event;
    }

    // --- Getters (immutable, no setters) ---

    public Long getId() { return id; }
    public Finding getFinding() { return finding; }
    public EventAction getAction() { return action; }
    public FindingStatus getFromStatus() { return fromStatus; }
    public FindingStatus getToStatus() { return toStatus; }
    public ActorType getActorType() { return actorType; }
    public AppUser getActor() { return actor; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
