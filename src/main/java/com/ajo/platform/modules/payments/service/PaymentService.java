package com.ajo.platform.modules.payments.service;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.auth.repository.UserRepository;
import com.ajo.platform.modules.contributions.model.Contribution;
import com.ajo.platform.modules.contributions.repository.ContributionRepository;
import com.ajo.platform.modules.groups.model.Group;
import com.ajo.platform.modules.groups.repository.GroupMemberRepository;
import com.ajo.platform.modules.groups.repository.GroupRepository;
import com.ajo.platform.modules.payments.config.PaystackConfig;
import com.ajo.platform.modules.payments.dto.InitializePaymentRequest;
import com.ajo.platform.modules.payments.dto.InitializePaymentResponse;
import com.ajo.platform.modules.payments.dto.PaystackWebhookEvent;
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

        // Amount in kobo (Paystack uses kobo)
        long amountInKobo = group.getContributionAmount()
                .multiply(BigDecimal.valueOf(100)).longValue();

        // Build metadata to identify the contribution on webhook
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

        // Verify webhook signature
        if (!isValidSignature(payload, paystackSignature)) {
            throw new RuntimeException("Invalid webhook signature");
        }

        PaystackWebhookEvent event = objectMapper.readValue(payload,
                PaystackWebhookEvent.class);

        if (!"charge.success".equals(event.getEvent())) {
            log.info("Ignoring webhook event: {}", event.getEvent());
            return;
        }

        PaystackWebhookEvent.EventData data = event.getData();
        PaystackWebhookEvent.Metadata metadata = data.getMetadata();

        if (metadata == null) {
            log.warn("Webhook received without metadata, reference: {}", data.getReference());
            return;
        }

        Long groupId = metadata.getGroupId();
        Integer cycleNumber = metadata.getCycleNumber();
        Long userId = metadata.getUserId();

        // Idempotency check
        String idempotencyKey = "contrib-" + userId + "-" + groupId + "-" + cycleNumber;
        if (contributionRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.info("Contribution already exists for key: {}", idempotencyKey);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Contribution contribution = Contribution.builder()
                .group(group)
                .user(user)
                .cycleNumber(cycleNumber)
                .amount(group.getContributionAmount())
                .status(Contribution.ContributionStatus.PAID)
                .idempotencyKey(idempotencyKey)
                .paymentReference(data.getReference())
                .paidAt(LocalDateTime.now())
                .build();

        contributionRepository.save(contribution);

        log.info("Contribution recorded for user {} in group {} cycle {}",
                userId, groupId, cycleNumber);
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