package com.ajo.platform.modules.payouts.service;

import com.ajo.platform.modules.auth.model.User;
import com.ajo.platform.modules.auth.repository.UserRepository;
import com.ajo.platform.modules.contributions.repository.ContributionRepository;
import com.ajo.platform.modules.groups.model.Group;
import com.ajo.platform.modules.groups.model.GroupMember;
import com.ajo.platform.modules.groups.repository.GroupMemberRepository;
import com.ajo.platform.modules.groups.repository.GroupRepository;
import com.ajo.platform.modules.payments.config.PaystackConfig;
import com.ajo.platform.modules.payouts.dto.PayoutResponse;
import com.ajo.platform.modules.payouts.dto.PayoutSummary;
import com.ajo.platform.modules.payouts.model.Payout;
import com.ajo.platform.modules.payouts.repository.PayoutRepository;
import com.ajo.platform.modules.users.model.BankAccount;
import com.ajo.platform.modules.users.repository.BankAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ContributionRepository contributionRepository;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PaystackConfig paystackConfig;
    private final ObjectMapper objectMapper;

    @Transactional
    public PayoutResponse triggerPayout(Long groupId, Integer cycleNumber, String email) {

        User requestingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        if (!group.getCreatedBy().getId().equals(requestingUser.getId())) {
            throw new RuntimeException("Only the group creator can trigger payouts");
        }

        if (payoutRepository.existsByGroupIdAndCycleNumber(groupId, cycleNumber)) {
            throw new RuntimeException("Payout already exists for this cycle");
        }

        int totalMembers = groupMemberRepository.countByGroupId(groupId);
        int paidMembers = contributionRepository.countPaidContributions(groupId, cycleNumber);

        if (paidMembers < totalMembers) {
            throw new RuntimeException("Not all members have paid for cycle "
                    + cycleNumber + ". Paid: " + paidMembers + "/" + totalMembers);
        }

        GroupMember recipientMember = groupMemberRepository.findByGroupId(groupId)
                .stream()
                .filter(m -> m.getPayoutPosition().equals(cycleNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No member found for payout position " + cycleNumber));

        User recipient = recipientMember.getUser();

        BankAccount bankAccount = bankAccountRepository.findByUserId(recipient.getId())
                .orElseThrow(() -> new RuntimeException(
                        recipient.getFirstName() + " has no saved bank account. They must add one before receiving a payout."));

        BigDecimal payoutAmount = group.getContributionAmount()
                .multiply(BigDecimal.valueOf(totalMembers));

        String narration = "Ajo payout for " + group.getName() + " - Cycle " + cycleNumber;

        try {
            String recipientCode = createTransferRecipient(bankAccount);
            String transferCode = initiateTransfer(recipientCode, payoutAmount, narration);

            Payout payout = Payout.builder()
                    .group(group)
                    .recipient(recipient)
                    .cycleNumber(cycleNumber)
                    .amount(payoutAmount)
                    .status(Payout.PayoutStatus.PROCESSING)
                    .paymentReference(transferCode)
                    .narration(narration)
                    .build();

            payoutRepository.save(payout);

            if (cycleNumber >= group.getMaxMembers()) {
                group.setStatus(Group.GroupStatus.COMPLETED);
                groupRepository.save(group);
            }

            return mapToResponse(payout);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Transfer initiation failed: " + e.getMessage(), e);
        }
    }

    public PayoutSummary getCycleSummary(Long groupId, Integer cycleNumber, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        int totalMembers = groupMemberRepository.countByGroupId(groupId);
        int paidMembers = contributionRepository.countPaidContributions(groupId, cycleNumber);

        GroupMember recipientMember = groupMemberRepository.findByGroupId(groupId)
                .stream()
                .filter(m -> m.getPayoutPosition().equals(cycleNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No member found for position " + cycleNumber));

        BigDecimal payoutAmount = group.getContributionAmount()
                .multiply(BigDecimal.valueOf(totalMembers));

        String status = payoutRepository.findByGroupIdAndCycleNumber(groupId, cycleNumber)
                .map(p -> p.getStatus().name())
                .orElse("PENDING");

        return PayoutSummary.builder()
                .cycleNumber(cycleNumber)
                .recipientName(recipientMember.getUser().getFirstName()
                        + " " + recipientMember.getUser().getLastName())
                .amount(payoutAmount)
                .status(status)
                .allMembersPaid(paidMembers == totalMembers)
                .paidMembersCount(paidMembers)
                .totalMembers(totalMembers)
                .build();
    }

    public List<PayoutResponse> getGroupPayouts(Long groupId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new RuntimeException("You are not a member of this group");
        }

        return payoutRepository.findByGroupId(groupId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PayoutResponse> getMyPayouts(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return payoutRepository.findByRecipientId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String createTransferRecipient(BankAccount bankAccount) throws Exception {
        Map<String, Object> payload = Map.of(
                "type", "nuban",
                "name", bankAccount.getAccountName(),
                "account_number", bankAccount.getAccountNumber(),
                "bank_code", bankAccount.getBankCode(),
                "currency", "NGN"
        );

        String json = objectMapper.writeValueAsString(payload);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(paystackConfig.getBaseUrl() + "/transferrecipient"))
                .header("Authorization", "Bearer " + paystackConfig.getSecretKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode body = objectMapper.readTree(response.body());

        if (!body.get("status").asBoolean()) {
            throw new RuntimeException("Failed to create transfer recipient: " + body.get("message").asText());
        }

        return body.get("data").get("recipient_code").asText();
    }

    private String initiateTransfer(String recipientCode, BigDecimal amount, String narration) throws Exception {
        long amountInKobo = amount.multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, Object> payload = Map.of(
                "source", "balance",
                "amount", amountInKobo,
                "recipient", recipientCode,
                "reason", narration
        );

        String json = objectMapper.writeValueAsString(payload);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(paystackConfig.getBaseUrl() + "/transfer"))
                .header("Authorization", "Bearer " + paystackConfig.getSecretKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode body = objectMapper.readTree(response.body());

        if (!body.get("status").asBoolean()) {
            throw new RuntimeException("Transfer initiation failed: " + body.get("message").asText());
        }

        return body.get("data").get("transfer_code").asText();
    }

    private PayoutResponse mapToResponse(Payout payout) {
        return PayoutResponse.builder()
                .id(payout.getId())
                .groupId(payout.getGroup().getId())
                .groupName(payout.getGroup().getName())
                .recipient(PayoutResponse.RecipientInfo.builder()
                        .id(payout.getRecipient().getId())
                        .firstName(payout.getRecipient().getFirstName())
                        .lastName(payout.getRecipient().getLastName())
                        .build())
                .recipientName(payout.getRecipient().getFirstName()
                        + " " + payout.getRecipient().getLastName())
                .recipientEmail(payout.getRecipient().getEmail())
                .cycleNumber(payout.getCycleNumber())
                .amount(payout.getAmount())
                .status(payout.getStatus())
                .paymentReference(payout.getPaymentReference())
                .narration(payout.getNarration())
                .createdAt(payout.getCreatedAt())
                .disbursedAt(payout.getDisbursedAt())
                .build();
    }
}