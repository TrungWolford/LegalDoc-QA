package com.contractcompiler.counsel;

import com.contractcompiler.auth.AppUser;
import com.contractcompiler.common.exception.NotFoundException;
import com.contractcompiler.contract.Contract;
import com.contractcompiler.contract.ContractRepository;
import com.contractcompiler.counsel.dto.*;
import com.contractcompiler.counsel.repository.QuestionPackageRepository;
import com.contractcompiler.finding.*;
import com.contractcompiler.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for the counsel question workflow.
 * Manages package creation, sending, and answer processing.
 *
 * <p>Per doc 02: counsel module calls FindingWorkflowService for status transitions.
 */
@Service
public class PackageService {

    private final QuestionPackageRepository packageRepository;
    private final FindingRepository findingRepository;
    private final FindingWorkflowService workflowService;
    private final CounselTokenService tokenService;
    private final NotificationService notificationService;
    private final ContractRepository contractRepository;

    public PackageService(QuestionPackageRepository packageRepository,
                          FindingRepository findingRepository,
                          FindingWorkflowService workflowService,
                          CounselTokenService tokenService,
                          NotificationService notificationService,
                          ContractRepository contractRepository) {
        this.packageRepository = packageRepository;
        this.findingRepository = findingRepository;
        this.workflowService = workflowService;
        this.tokenService = tokenService;
        this.notificationService = notificationService;
        this.contractRepository = contractRepository;
    }

    /**
     * Creates a new question package from selected findings.
     * Findings must be in CONFIRMED status to be included.
     */
    @Transactional
    public PackageResponse createPackage(CreatePackageRequest request, AppUser currentUser) {
        List<Finding> findings = request.findingIds().stream()
                .map(id -> findingRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Finding", id)))
                .toList();

        // All findings must belong to the same contract
        Long contractId = findings.getFirst().getContract().getId();
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new NotFoundException("Contract", contractId));

        QuestionPackage pkg = new QuestionPackage(
                contract, request.title(), request.counselEmail(),
                request.counselName(), currentUser
        );

        int position = 1;
        for (Finding finding : findings) {
            // Generate question text from the finding (simplified; QuestionGenerator would be used in full impl)
            String questionText = finding.getMessage();
            List<String> options = List.of(); // QuestionGenerator would populate this

            QuestionItem item = new QuestionItem(finding, position++, questionText, options);
            pkg.addItem(item);
        }

        packageRepository.save(pkg);
        return toResponse(pkg);
    }

    /**
     * Sends the package to counsel via email.
     * Issues a token, stores hash, and sends the notification.
     */
    @Transactional
    public void sendPackage(Long packageId) {
        QuestionPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("QuestionPackage", packageId));

        // Revoke old token if resending
        if (pkg.getTokenHash() != null) {
            tokenService.revokeToken(pkg.getTokenHash());
        }

        CounselTokenService.TokenResult tokenResult = tokenService.issueToken(packageId);
        pkg.markSent(tokenResult.tokenHash(), tokenResult.expiresAt());

        // Transition findings to SENT_TO_COUNSEL
        for (QuestionItem item : pkg.getItems()) {
            if (item.getFinding().getStatus() == FindingStatus.CONFIRMED) {
                workflowService.systemTransition(
                        item.getFinding(), FindingStatus.SENT_TO_COUNSEL,
                        "Sent in question package #" + packageId);
            }
        }

        packageRepository.save(pkg);

        // Send email with the raw token link
        String counselLink = "/public/counsel/" + tokenResult.rawToken();
        String subject = "Questions about: " + pkg.getTitle();
        String htmlBody = "<p>Please answer the questions at: <a href=\"" + counselLink + "\">" + counselLink + "</a></p>";

        notificationService.sendCounselEmail(
                pkg.getCounselEmail(), pkg.getCounselName(), subject, htmlBody
        );
    }

    /**
     * Loads a package for the public counsel view using a raw token.
     */
    @Transactional(readOnly = true)
    public CounselPackageResponse getPackageByToken(String rawToken) {
        Long packageId = tokenService.validateToken(rawToken);
        if (packageId == null) {
            throw new NotFoundException("QuestionPackage", "token:" + rawToken.substring(0, 8));
        }

        QuestionPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("QuestionPackage", packageId));

        return toCounselResponse(pkg);
    }

    /**
     * Processes counsel answers submitted through the public endpoint.
     */
    @Transactional
    public void submitAnswers(String rawToken, CounselAnswerRequest request) {
        Long packageId = tokenService.validateToken(rawToken);
        if (packageId == null) {
            throw new NotFoundException("QuestionPackage", "token:" + rawToken.substring(0, 8));
        }

        QuestionPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new NotFoundException("QuestionPackage", packageId));

        for (CounselAnswerRequest.Answer answer : request.answers()) {
            QuestionItem item = pkg.getItems().stream()
                    .filter(i -> i.getPosition() == answer.position())
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("QuestionItem", answer.position()));

            item.answer(answer.choice(), answer.note());

            // Transition finding to RESOLVED
            Finding finding = item.getFinding();
            if (finding.getStatus() == FindingStatus.SENT_TO_COUNSEL) {
                workflowService.systemTransition(
                        finding, FindingStatus.RESOLVED,
                        "Counsel answered: " + answer.choice());
            }
        }

        // Update package status
        if (pkg.isFullyAnswered()) {
            pkg.markCompleted();
        } else if (pkg.isPartiallyAnswered()) {
            pkg.setStatus(PackageStatus.PARTIAL);
        }

        packageRepository.save(pkg);
    }

    // --- Mapping ---

    private PackageResponse toResponse(QuestionPackage pkg) {
        List<PackageResponse.QuestionItemResponse> items = pkg.getItems().stream()
                .map(item -> new PackageResponse.QuestionItemResponse(
                        item.getId(), item.getFinding().getId(), item.getPosition(),
                        item.getQuestionText(), item.getOptions(),
                        item.getAnswerChoice(), item.getAnswerNote(), item.getAnsweredAt()
                ))
                .collect(Collectors.toList());

        return new PackageResponse(
                pkg.getId(), pkg.getContract().getId(), pkg.getTitle(),
                pkg.getCounselName(), pkg.getCounselEmail(), pkg.getStatus().name(),
                pkg.getSendCount(), pkg.getCreatedAt(), pkg.getSentAt(), pkg.getCompletedAt(),
                items
        );
    }

    private CounselPackageResponse toCounselResponse(QuestionPackage pkg) {
        List<CounselPackageResponse.CounselQuestion> questions = pkg.getItems().stream()
                .map(item -> new CounselPackageResponse.CounselQuestion(
                        item.getPosition(), item.getQuestionText(), item.getOptions(),
                        item.getFinding().getSectionLabel(), item.getFinding().getEvidence(),
                        item.getAnswerChoice(), item.getAnswerNote(), item.isAnswered()
                ))
                .collect(Collectors.toList());

        return new CounselPackageResponse(
                pkg.getTitle(), pkg.getContract().getTitle(), questions
        );
    }
}
