package com.ajo.platform.modules.payouts.repository;

import com.ajo.platform.modules.payouts.model.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Optional<Payout> findByGroupIdAndCycleNumber(Long groupId, Integer cycleNumber);

    List<Payout> findByGroupId(Long groupId);

    List<Payout> findByRecipientId(Long recipientId);

    boolean existsByGroupIdAndCycleNumber(Long groupId, Integer cycleNumber);

    Optional<Payout> findByPaymentReference(String paymentReference);
}