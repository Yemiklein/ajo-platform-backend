package com.ajo.platform.modules.contributions.repository;

import com.ajo.platform.modules.contributions.model.Contribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    boolean existsByGroupIdAndUserIdAndCycleNumber(
            Long groupId, Long userId, Integer cycleNumber);

    Optional<Contribution> findByIdempotencyKey(String idempotencyKey);

    List<Contribution> findByGroupIdAndCycleNumber(Long groupId, Integer cycleNumber);

    List<Contribution> findByGroupIdAndUserId(Long groupId, Long userId);

    @Query("SELECT COUNT(c) FROM Contribution c WHERE c.group.id = :groupId " +
            "AND c.cycleNumber = :cycleNumber AND c.status = com.ajo.platform.modules.contributions.model.Contribution.ContributionStatus.PAID")
    int countPaidContributions(
            @Param("groupId") Long groupId,
            @Param("cycleNumber") Integer cycleNumber);
}