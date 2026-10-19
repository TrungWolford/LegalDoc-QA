package com.contractcompiler.finding;

import com.contractcompiler.auth.AppUser;
import com.contractcompiler.common.exception.InvalidTransitionException;
import com.contractcompiler.common.exception.NotFoundException;
import com.contractcompiler.finding.dto.FindingEventResponse;
import com.contractcompiler.finding.dto.FindingResponse;
import com.contractcompiler.finding.dto.StatusChangeRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The single entry point for all finding status transitions.
 * Enforces the state machine, records audit events, and uses optimistic locking.
 *
 * <p>Per doc 02: "Mọi thay đổi trạng thái đi qua một service"
 * and "Status changed anywhere except FindingWorkflowService" is an anti-pattern.
 *
 * <p>Transition rules per doc 03:
 * <pre>
 * DETECTED → CONFIRMED, DISMISSED
 * CONFIRMED → SENT_TO_COUNSEL, RESOLVED, DISMISSED
 * SENT_TO_COUNSEL → RESOLVED, DISMISSED, CONFIRMED (revoke on expiry)
 * DISMISSED → DETECTED (reviewer changed mind)
 * RESOLVED → (terminal)
 * </pre>
 */
@Service
public class FindingWorkflowService {

    private static final Map<FindingStatus, Set<FindingStatus>> ALLOWED_TRANSITIONS;

    static {
        ALLOWED_TRANSITIONS = new EnumMap<>(FindingStatus.class);
        ALLOWED_TRANSITIONS.put(FindingStatus.DETECTED,
                Set.of(FindingStatus.CONFIRMED, FindingStatus.DISMISSED));
        ALLOWED_TRANSITIONS.put(FindingStatus.CONFIRMED,
                Set.of(FindingStatus.SENT_TO_COUNSEL, FindingStatus.RESOLVED, FindingStatus.DISMISSED));
        ALLOWED_TRANSITIONS.put(FindingStatus.SENT_TO_COUNSEL,
                Set.of(FindingStatus.RESOLVED, FindingStatus.DISMISSED, FindingStatus.CONFIRMED));
        ALLOWED_TRANSITIONS.put(FindingStatus.DISMISSED,
                Set.of(FindingStatus.DETECTED));
        ALLOWED_TRANSITIONS.put(FindingStatus.RESOLVED,
                Set.of());
    }

    private final FindingRepository findingRepository;
    private final FindingEventRepository eventRepository;

    public FindingWorkflowService(FindingRepository findingRepository,
                                  FindingEventRepository eventRepository) {
        this.findingRepository = findingRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Transitions a finding to a new status.
     * Validates the transition, updates the finding, and records an audit event.
     * All in a single transaction.
     */
    @Transactional
    public FindingResponse changeStatus(Long findingId, StatusChangeRequest request, AppUser actor) {
        Finding finding = findingRepository.findById(findingId)
                .orElseThrow(() -> new NotFoundException("Finding", findingId));

        FindingStatus fromStatus = finding.getStatus();
        FindingStatus toStatus = FindingStatus.valueOf(request.toStatus());

        // Validate the transition
        Set<FindingStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(fromStatus, Set.of());
        if (!allowed.contains(toStatus)) {
            throw new InvalidTransitionException(fromStatus.name(), toStatus.name());
        }

        // Perform the transition
        finding.transitionTo(toStatus, request.note());
        findingRepository.save(finding);

        // Record the audit event
        FindingEvent event = FindingEvent.statusChanged(
                finding, fromStatus, toStatus, ActorType.USER, actor, request.note());
        eventRepository.save(event);

        return toResponse(finding);
    }

    /**
     * Transitions a finding by the system (e.g. during counsel workflow).
     */
    @Transactional
    public void systemTransition(Finding finding, FindingStatus toStatus, String note) {
        FindingStatus fromStatus = finding.getStatus();

        Set<FindingStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(fromStatus, Set.of());
        if (!allowed.contains(toStatus)) {
            throw new InvalidTransitionException(fromStatus.name(), toStatus.name());
        }

        finding.transitionTo(toStatus, note);
        findingRepository.save(finding);

        FindingEvent event = FindingEvent.statusChanged(
                finding, fromStatus, toStatus, ActorType.SYSTEM, null, note);
        eventRepository.save(event);
    }

    /**
     * Retrieves the audit trail for a finding.
     */
    @Transactional(readOnly = true)
    public List<FindingEventResponse> getEvents(Long findingId) {
        if (!findingRepository.existsById(findingId)) {
            throw new NotFoundException("Finding", findingId);
        }

        return eventRepository.findByFindingIdOrderByCreatedAtAsc(findingId).stream()
                .map(this::toEventResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns filtered findings for a version.
     */
    @Transactional(readOnly = true)
    public List<FindingResponse> listFindings(Long versionId, String severity,
                                               String status, String ruleId) {
        Severity sevFilter = severity != null ? Severity.valueOf(severity) : null;
        FindingStatus statusFilter = status != null ? FindingStatus.valueOf(status) : null;

        return findingRepository.findByVersionFiltered(versionId, sevFilter, statusFilter, ruleId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // --- Mapping helpers ---

    public FindingResponse toResponse(Finding f) {
        return new FindingResponse(
                f.getId(), f.getRuleId(), f.getSource().name(),
                f.getSeverity().name(), f.getStatus().name(),
                f.isNeedsCounsel(), f.getSectionLabel(),
                f.getEvidence(), f.getEvidenceStart(), f.getEvidenceEnd(),
                f.getPage(), f.getMessage(), f.getDetails(),
                f.getConfidence(), f.getFingerprint(),
                f.getCarriedFrom() != null ? f.getCarriedFrom().getId() : null,
                f.getResolutionNote(),
                f.getAssignee() != null ? f.getAssignee().getFullName() : null,
                f.getLockVersion(),
                f.getCreatedAt(), f.getUpdatedAt()
        );
    }

    private FindingEventResponse toEventResponse(FindingEvent e) {
        return new FindingEventResponse(
                e.getId(), e.getAction().name(),
                e.getFromStatus() != null ? e.getFromStatus().name() : null,
                e.getToStatus() != null ? e.getToStatus().name() : null,
                e.getActorType().name(),
                e.getActor() != null ? e.getActor().getFullName() : null,
                e.getNote(), e.getCreatedAt()
        );
    }
}
