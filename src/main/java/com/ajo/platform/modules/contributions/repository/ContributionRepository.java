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

    // ============ EXISTING METHODS ============

    boolean existsByGroupIdAndUserIdAndCycleNumber(Long groupId, Long userId, Integer cycleNumber);

    boolean existsByGroupIdAndUserIdAndCycleNumberAndStatus(Long groupId, Long userId, Integer cycleNumber, String status);

    Optional<Contribution> findByGroupIdAndUserIdAndCycleNumber(Long groupId, Long userId, Integer cycleNumber);

    List<Contribution> findByGroupIdAndCycleNumber(Long groupId, Integer cycleNumber);

    List<Contribution> findByGroupId(Long groupId);

    int countByGroupIdAndUserIdAndStatus(Long groupId, Long userId, String status);

    // ============ NEW MISSING METHODS ============

    // For idempotency key lookups (prevents duplicate payments)
    Optional<Contribution> findByIdempotencyKey(String idempotencyKey);

    // Find contribution by group and user (without cycle - gets latest or all)
    List<Contribution> findByGroupIdAndUserId(Long groupId, Long userId);

    // Count paid contributions for a specific group and cycle
    @Query("SELECT COUNT(c) FROM Contribution c WHERE c.group.id = :groupId AND c.cycleNumber = :cycleNumber AND c.status = 'PAID'")
    int countPaidContributions(@Param("groupId") Long groupId, @Param("cycleNumber") Integer cycleNumber);

    // Alternative method name that Spring Data JPA can derive
    int countByGroupIdAndCycleNumberAndStatus(Long groupId, Integer cycleNumber, String status);

    // ============ AGGREGATE METHODS ============

    @Query("SELECT SUM(c.amount) FROM Contribution c WHERE c.group.id = :groupId AND c.status = :status")
    Optional<BigDecimal> sumAmountByGroupIdAndStatus(@Param("groupId") Long groupId, @Param("status") String status);

    @Query("SELECT SUM(c.amount) FROM Contribution c WHERE c.group.id = :groupId AND c.user.id = :userId AND c.status = :status")
    Optional<BigDecimal> sumAmountByGroupIdAndUserIdAndStatus(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT MAX(c.paidAt) FROM Contribution c WHERE c.user.id = :userId AND c.group.id = :groupId AND c.status = 'PAID'")
    Optional<LocalDateTime> findLastPaymentDateByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Query("SELECT COUNT(DISTINCT c.user.id) FROM Contribution c WHERE c.group.id = :groupId AND c.cycleNumber = :cycleNumber AND c.status = 'PAID'")
    int countDistinctUsersWhoPaidByGroupAndCycle(@Param("groupId") Long groupId, @Param("cycleNumber") Integer cycleNumber);

    // ============ ADDITIONAL USEFUL METHODS ============

    // Find all contributions by user
    List<Contribution> findByUserId(Long userId);

    // Find all contributions by group and status
    List<Contribution> findByGroupIdAndStatus(Long groupId, String status);

    // Check if user has any paid contribution in group
    boolean existsByGroupIdAndUserIdAndStatus(Long groupId, Long userId, String status);
}