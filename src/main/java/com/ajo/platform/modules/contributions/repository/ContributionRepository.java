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

    // Basic existence checks
    boolean existsByGroupIdAndUserIdAndCycleNumber(Long groupId, Long userId, Integer cycleNumber);

    boolean existsByGroupIdAndUserIdAndCycleNumberAndStatus(Long groupId, Long userId, Integer cycleNumber, String status);

    // Find methods
    Optional<Contribution> findByGroupIdAndUserIdAndCycleNumber(Long groupId, Long userId, Integer cycleNumber);

    List<Contribution> findByGroupIdAndCycleNumber(Long groupId, Integer cycleNumber);

    List<Contribution> findByGroupId(Long groupId);

    // Count methods
    int countByGroupIdAndUserIdAndStatus(Long groupId, Long userId, String status);

    // Aggregate queries using @Query
    @Query("SELECT SUM(c.amount) FROM Contribution c WHERE c.group.id = :groupId AND c.status = :status")
    Optional<BigDecimal> sumAmountByGroupIdAndStatus(@Param("groupId") Long groupId, @Param("status") String status);

    @Query("SELECT SUM(c.amount) FROM Contribution c WHERE c.group.id = :groupId AND c.user.id = :userId AND c.status = :status")
    Optional<BigDecimal> sumAmountByGroupIdAndUserIdAndStatus(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT MAX(c.paidAt) FROM Contribution c WHERE c.user.id = :userId AND c.group.id = :groupId AND c.status = 'PAID'")
    Optional<LocalDateTime> findLastPaymentDateByUserIdAndGroupId(@Param("userId") Long userId, @Param("groupId") Long groupId);

    @Query("SELECT COUNT(DISTINCT c.user.id) FROM Contribution c WHERE c.group.id = :groupId AND c.cycleNumber = :cycleNumber AND c.status = 'PAID'")
    int countDistinctUsersWhoPaidByGroupAndCycle(@Param("groupId") Long groupId, @Param("cycleNumber") Integer cycleNumber);
}