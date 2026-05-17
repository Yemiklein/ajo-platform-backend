package com.ajo.platform.modules.contributions.repository;

import com.ajo.platform.modules.contributions.model.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    // ============ BASIC QUERIES ============

    boolean existsByGroupIdAndUserIdAndCycleNumber(Long groupId, Long userId, Integer cycleNumber);

    boolean existsByGroupIdAndUserIdAndCycleNumberAndStatus(Long groupId, Long userId, Integer cycleNumber, String status);

    Optional<Contribution> findByGroupIdAndUserIdAndCycleNumber(Long groupId, Long userId, Integer cycleNumber);

    List<Contribution> findByGroupIdAndCycleNumber(Long groupId, Integer cycleNumber);

    List<Contribution> findByGroupId(Long groupId);

    List<Contribution> findByGroupIdAndUserId(Long groupId, Long userId);

    Optional<Contribution> findByIdempotencyKey(String idempotencyKey);

    // ============ QUERIES FOR PAYOUT SERVICE ============

    @Query("SELECT COUNT(c) FROM Contribution c WHERE c.group.id = :groupId AND c.cycleNumber = :cycleNumber AND c.status = 'PAID'")
    int countPaidContributions(@Param("groupId") Long groupId, @Param("cycleNumber") Integer cycleNumber);

    // ============ QUERIES FOR GROUP SERVICE ============

    @Query("SELECT COUNT(c) FROM Contribution c WHERE c.group.id = :groupId AND c.user.id = :userId AND c.status = 'PAID'")
    int countByGroupIdAndUserIdAndStatus(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT SUM(c.amount) FROM Contribution c WHERE c.group.id = :groupId AND c.status = 'PAID'")
    Optional<BigDecimal> sumAmountByGroupIdAndStatus(@Param("groupId") Long groupId);

    @Query("SELECT SUM(c.amount) FROM Contribution c WHERE c.group.id = :groupId AND c.user.id = :userId AND c.status = 'PAID'")
    Optional<BigDecimal> sumAmountByGroupIdAndUserIdAndStatus(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT MAX(c.paidAt) FROM Contribution c WHERE c.user.id = :userId AND c.group.id = :groupId AND c.status = 'PAID'")
    Optional<LocalDateTime> findLastPaymentDateByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Query("SELECT COUNT(c) FROM Contribution c WHERE c.group.id = :groupId AND c.status = 'PAID'")
    long countTotalPaidContributions(@Param("groupId") Long groupId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM Contribution c WHERE c.group.id = :groupId AND c.status = 'PAID'")
    BigDecimal sumPaidContributions(@Param("groupId") Long groupId);
}