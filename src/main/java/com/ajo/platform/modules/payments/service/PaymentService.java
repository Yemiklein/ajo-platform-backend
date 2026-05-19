package com.ajo.platform.modules.payments.service;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.auth.repository.UserRepository;
import com.ajo.platform.modules.contributions.model.Contribution;
import com.ajo.platform.modules.contributions.repository.ContributionRepository;
import com.ajo.platform.modules.groups.model.Group;
import com.ajo.platform.modules.groups.repository.GroupMemberRepository;
import com.ajo.platform.modules.groups.repository.GroupRepository;
import com.ajo.platform.modules.notifications.service.NotificationService;
import com.ajo.platform.modules.payments.config.PaystackConfig;
import com.ajo.platform.modules.payments.dto.InitializePaymentRequest;
import com.ajo.platform.modules.payments.dto.InitializePaymentResponse;
import com.ajo.platform.modules.payouts.model.Payout;
import com.ajo.platform.modules.payouts.repository.PayoutRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaystackConfig paystackConfig;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ContributionRepository contributionRepository;
    private final PayoutRepository payoutRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public InitializePaymentResponse initializePayment(
            InitializePaymentRequest request, String email) throws Exception {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), user.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        if (group.getStatus() != Group.GroupStatus.ACTIVE) {
            throw new RuntimeException("Group is not active");
        }

        if (contributionRepository.existsByGroupIdAndUserIdAndCycleNumber(
                group.getId(), user.getId(), request.getCycleNumber())) {
            throw new RuntimeException("You have already contributed for this cycle");
        }

        long amountInKobo = group.getContributionAmount()
                .multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, Object> metadata = Map.of(
                "group_id", group.getId(),
                "cycle_number", request.getCycleNumber(),
                "user_id", user.getId()
        );

        Map<String, Object> payload = Map.of(
                "email", user.getEmail(),
                "amount", amountInKobo,
                "metadata", metadata,
                "callback_url", "https://ajo-platform-frontend.vercel.app/payments/callback"
        );

        String payloadJson = objectMapper.writeValueAsString(payload);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(paystackConfig.getBaseUrl() + "/transaction/initialize"))
                .header("Authorization", "Bearer " + paystackConfig.getSecretKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                .build();

        HttpResponse<String> response = client.send(httpRequest,
                HttpResponse.BodyHandlers.ofString());

        JsonNode responseBody = objectMapper.readTree(response.body());

        if (!responseBody.get("status").asBoolean()) {
            throw new RuntimeException("Payment initialization failed: "
                    + responseBody.get("message").asText());
        }

        JsonNode data = responseBody.get("data");

        return InitializePaymentResponse.builder()
                .authorizationUrl(data.get("authorization_url").asText())
                .accessCode(data.get("access_code").asText())
                .reference(data.get("reference").asText())
                .build();
    }

    @Transactional
    public void handleWebhook(String payload, String paystackSignature) throws Exception {

        if (!isValidSignature(payload, paystackSignature)) {
            throw new RuntimeException("Invalid webhook signature");
        }

        JsonNode root = objectMapper.readTree(payload);
        String eventType = root.get("event").asText();
        JsonNode data = root.get("data");

        switch (eventType) {
            case "charge.success" -> handleChargeSuccess(data);
            case "transfer.success" -> handleTransferSuccess(data);
            case "transfer.failed" -> handleTransferFailed(data);
            default -> log.info("Ignoring webhook event: {}", eventType);
        }
    }

    private void handleChargeSuccess(JsonNode data) {
        JsonNode metadataNode = data.get("metadata");
        if (metadataNode == null || metadataNode.isNull()) {
            log.warn("charge.success webhook without metadata, reference: {}",
                    data.get("reference").asText());
            return;
        }

        Long groupId = metadataNode.get("group_id").asLong();
        Integer cycleNumber = metadataNode.get("cycle_number").asInt();
        Long userId = metadataNode.get("user_id").asLong();
        String reference = data.get("reference").asText();

        String idempotencyKey = "contrib-" + userId + "-" + groupId + "-" + cycleNumber;
        if (contributionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.info("Contribution already exists for key: {}", idempotencyKey);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));

        Contribution contribution = Contribution.builder()
                .group(group)
                .user(user)
                .cycleNumber(cycleNumber)
                .amount(group.getContributionAmount())
                .status(Contribution.ContributionStatus.PAID)
                .idempotencyKey(idempotencyKey)
                .paymentReference(reference)
                .paidAt(LocalDateTime.now())
                .build();

        contributionRepository.save(contribution);
        log.info("Contribution recorded for user {} in group {} cycle {}", userId, groupId, cycleNumber);

        int totalMembers = groupMemberRepository.countByGroupId(groupId);
        int paidCount = contributionRepository.countPaidContributions(groupId, cycleNumber);

        if (paidCount >= totalMembers) {
            User creator = group.getCreatedBy();
            notificationService.sendEmailNotification(
                    creator.getEmail(),
                    "All members paid — " + group.getName(),
                    String.format("All %d members have paid for cycle %d in '%s'. You can now trigger the payout.",
                            totalMembers, cycleNumber, group.getName())
            );
            log.info("All members paid for group {} cycle {}, creator notified", groupId, cycleNumber);
        }
    }

    private void handleTransferSuccess(JsonNode data) {
        String transferCode = data.get("transfer_code").asText();
        payoutRepository.findByPaymentReference(transferCode).ifPresentOrElse(payout -> {
            payout.setStatus(Payout.PayoutStatus.COMPLETED);
            payout.setDisbursedAt(LocalDateTime.now());
            payoutRepository.save(payout);
            log.info("Payout {} marked COMPLETED for transfer {}", payout.getId(), transferCode);
        }, () -> log.warn("No payout found for transfer code: {}", transferCode));
    }

    private void handleTransferFailed(JsonNode data) {
        String transferCode = data.get("transfer_code").asText();
        payoutRepository.findByPaymentReference(transferCode).ifPresentOrElse(payout -> {
            payout.setStatus(Payout.PayoutStatus.FAILED);
            payoutRepository.save(payout);
            log.warn("Payout {} marked FAILED for transfer {}", payout.getId(), transferCode);
        }, () -> log.warn("No payout found for transfer code: {}", transferCode));
    }

    private boolean isValidSignature(String payload, String signature) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                paystackConfig.getSecretKey().getBytes(), "HmacSHA512");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes());
        String computedSignature = HexFormat.of().formatHex(hash);
        return computedSignature.equals(signature);
    }
}